# Animation import utility for Fate/Extella .mtb files
# Make sure animserv is running in the background before executing this script

# Configuration:

# Model and animation files path
# You can either enter absolutes paths or simply copy those files 
# next to current Blender file and only type their file names
mdlPath = "SV0000.mdl"
mtbPath = "SV0000_BS_WALK------_--_LP.mtb"

# Name of Blender skeleton to which animation data will be applied
blenderSkeletonTarget = "Armature"

# Set to 2 for 30 FPS, 3 for 60 FPS
# Higher values will generate more keyframes
framePartDiv = 3

# This will delete all previous animation data before importing
# Set to True to avoid weird artifacts
deleteOldKeyframes = True

# ----------------------------------------------

from struct import *
from pathlib import Path
import socket
import bpy

def importMtb():
    mdl = Path(mdlPath)
    mtb = Path(mtbPath)
    if not mdl.is_file():
        raise ValueError("MDL file does not exist")
    if not mtb.is_file():
        raise ValueError("MTB file does not exist")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 54217))
    mdlBoneNames = readMdlBoneList()
    frameParts = getFrameParts()
    framePartsCount = len(frameParts)
    if deleteOldKeyframes:
         bpy.data.objects[blenderSkeletonTarget].animation_data_clear()
    mtbFrameSetOffset, mtbFrameCount = readFrameSetInfo(mtbPath)
    ctx = bpy.context
    ctx.scene.frame_start = 0
    ctx.scene.frame_end = (mtbFrameCount + 1) * framePartsCount
    ctx.scene.frame_current = 0
    remoteLoadFile(sock, mtb)
    for idx in range(0, mtbFrameCount + 1):
        for partIdx, part in enumerate(frameParts):
            frameData = remoteDecodeFrame(sock, mtbFrameSetOffset, idx, mtbFrameCount, part)
            pushLocalFrame(frameData, mdlBoneNames, idx * framePartsCount + partIdx)
    sock.close()

def readFrameSetInfo(mtbPath):
    mtbFrameSetOffset = -1
    mtbFrameCount = -1
    with open(mtbPath, 'rb') as mtb:
        stream = LEInputStream(mtb)         
        stream.seek(0xC)
        mtbFrameSetOffset = stream.readShort()
        stream.seek(0x3C)
        frameSetHeader = stream.tell() + stream.readInt()
        stream.seek(frameSetHeader + 2)
        mtbFrameCount = stream.readShort()
    return mtbFrameSetOffset, mtbFrameCount

def getFrameParts():
    inc = 1.0 / framePartDiv
    cur = inc
    parts = []
    while cur < 1.0:
        parts.append(cur)
        cur += inc
        print(cur)
    print(parts)
    return parts

def readMdlBoneList():
    boneNames = []
    with open(mdlPath, 'rb') as mdl:
        stream = LEInputStream(mdl)
        stream.seek(0x1C)
        boneSectOffset = stream.readInt()
        stream.seek(boneSectOffset + 0x10)
        boneCount = stream.readInt()
        stream.seek(boneSectOffset + 0x30)
        boneNamesHeader = stream.tell() + stream.readInt() + 0x1C
        stream.seek(boneNamesHeader)
        boneNamesOffsets = []
        for idx in range(0, boneCount):
            boneNamesOffsets.append(stream.tell() + stream.readInt())
        for idx in range(0, boneCount):
            stream.seek(boneNamesOffsets[idx])
            boneNames.append(stream.readString())
    return boneNames

def pushLocalFrame(frameData, boneNames, frameIdx: int):
    for boneIdx, boneName in enumerate(boneNames):
        blenderBone = getBlenderBoneByName(boneName)
        if blenderBone is None:
            print("WARN: Missing bone %s" % boneName)
            continue
        boneOffset = boneIdx * 0x30
        qx, qy, qz, qw = unpack_from("<4f", frameData, offset=boneOffset)
        x, y, z, padding = unpack_from("<4f", frameData, offset=boneOffset+0x10)
        sx, sy, sz, padding = unpack_from("<4f", frameData, offset=boneOffset+0x20)        
        sf = 10.0 # scale factor
        blenderBone.location = [x / sf, y / sf, z / sf]
        blenderBone.keyframe_insert(data_path='location', frame=frameIdx)
        blenderBone.location = [sx / sf, sy / sf, sz / sf]
        blenderBone.keyframe_insert(data_path='scale', frame=frameIdx)
        blenderBone.rotation_quaternion = [qw, qx, qy, qz]
        blenderBone.keyframe_insert(data_path='rotation_quaternion', frame=frameIdx)

def getBlenderBoneByName(name):
    for blender_bone in bpy.data.objects[blenderSkeletonTarget].pose.bones.items():
        if blender_bone[0] == name:
            return blender_bone[1]

def remoteLoadFile(sock, path: Path):
    pathBytes = str(path.resolve()).replace("/", "\\").encode("utf-16le")
    if len(pathBytes) > 256:
        raise ValueError("Too long MTB path")
    sock.sendall(pack("<h", 0x3330))
    sock.sendall(pathBytes)
    sock.sendall(bytearray(256-len(pathBytes)))
    resId, resError = unpack("<hh", recvall(sock, 4))
    if not resId == 0x3331 or not resError == 0:
        raise ValueError("Invalid response after loading file")

def remoteDecodeFrame(sock, framesetDefOffset: int, frameIdx: int, frameCount: int, framePart: float):
    sock.sendall(pack("<h3if", 0x3332, framesetDefOffset, frameCount, frameIdx, framePart))
    resId, resError, dataSize = unpack("<2hi", recvall(sock, 8))
    if not resId == 0x3333 or not resError == 0:
        raise ValueError("Invalid response after decoding frame")
    decodedData = recvall(sock, dataSize)
    return decodedData

def recvall(sock, n):
    data = b''
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            return None
        data += packet
    return data

class LEInputStream:
    def __init__(self, stream):
        self.stream = stream

    def readByte(self):
        return unpack('b', self.stream.read(1))[0]

    def readShort(self):
        return unpack('<h', self.stream.read(2))[0]

    def readInt(self):
        return unpack('<i', self.stream.read(4))[0]
    
    def readString(self):
        data = bytearray()
        while True:
            byte = self.readByte()
            if byte == 0:
                break
            data.append(byte)
        return data.decode("ascii")
    
    def tell(self):
        return self.stream.tell()
    
    def seek(self, offset, whence=0):
        self.stream.seek(offset, whence)

importMtb()