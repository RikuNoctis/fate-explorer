# Animation import utility for Fate/Extella .mtb files

# Make sure animserv is running in the background before executing this script

# Configuration:

# Model and animation files path
# You can either enter absolutes paths or simply copy those files 
# next to current Blender file and only type their file names
# mtbPath can be a directory to batch import multiple .mtb
mdlPath = "ch002_m01_00.mdl"
mtbPath = "motion/ch002_m01_0202.mtb"

# Name of Blender skeleton to which animation data will be applied
blenderSkeletonTarget = "Armature"

# Set to 2 for 30 FPS, 3 for 60 FPS
# Higher values will generate more keyframes
framePartDiv = 3

# This will delete all previous animation data before importing
# Set to True to avoid weird artifacts when testing multiple animations
deleteOldKeyframes = True

# ----------------------------------------------

from struct import *
from pathlib import Path
import socket
import bpy
import os

def importMtb():
    mdl = Path(mdlPath)
    mtb = Path(mtbPath)
    blenderFileDir = os.path.dirname(bpy.data.filepath)
    os.chdir(blenderFileDir)
    if not mdl.is_file():
        raise ValueError("MDL file does not exist")
    if not mtb.is_file() and not mtb.is_dir():
        raise ValueError("MTB file or dir does not exist")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 54217))
    mdlBoneNames = readMdlBoneList()
    frameParts = getFrameParts()
    framePartsCount = len(frameParts)
    if deleteOldKeyframes:
         bpy.data.objects[blenderSkeletonTarget].animation_data_clear()
    ctx = bpy.context
    frameCount = 0
    
    mtbToImport = []
    if mtb.is_file():
        mtbToImport.append(mtb)
    else:
        mtbListing = mtb.glob('*.mtb')
        mtbToImport.extend([x for x in mtbListing if x.is_file()])
    
    for mtbFile in mtbToImport:
        mtbFrameSetOffsets, mtbFrameCounts = readFrameSetInfo(str(mtbFile.resolve()))
        remoteLoadFile(sock, mtbFile)
        for frameSetIdx in range(0, len(mtbFrameCounts)):
            if mtbFrameCounts[frameSetIdx] != 0:
                for idx in range(0, mtbFrameCounts[frameSetIdx] + 1):
                    for partIdx, part in enumerate(frameParts):
                        frameData = remoteDecodeFrame(sock, mtbFrameSetOffsets[frameSetIdx], idx, mtbFrameCounts[frameSetIdx], part)
                        pushLocalFrame(frameData, mdlBoneNames, frameCount)
                        frameCount += 1
            else:
                frameData = remoteDecodeFrame(sock, mtbFrameSetOffsets[frameSetIdx], 0, mtbFrameCounts[frameSetIdx], 0)
                pushLocalFrame(frameData, mdlBoneNames, frameCount)
                frameCount += 1   
    ctx.scene.frame_start = 0
    ctx.scene.frame_end = frameCount - 1
    ctx.scene.frame_current = 0
    sock.close()
    
def readFrameSetInfo(mtbPath):
    mtbFrameSetCount = -1
    mtbFrameSetInfoOffset1 = -1
    mtbFrameSetInfoOffset2 = -1
    mtbFrameSetOffsets = []
    mtbFrameSetFrameCounts = []
    with open(mtbPath, 'rb') as mtb:
        stream = LEInputStream(mtb)         
        stream.seek(0x12)
        mtbFrameSetCount = stream.readShort()
        stream.seek(0x38)
        mtbFrameSetInfoOffset1 = stream.tell() + stream.readInt()
        stream.seek(0x3C)
        mtbFrameSetInfoOffset2 = stream.tell() + stream.readInt()
        stream.seek(mtbFrameSetInfoOffset1)
        for idx in range(0, mtbFrameSetCount):
            stream.readInt()
            mtbFrameSetOffsets.append(stream.readInt())
        stream.seek(mtbFrameSetInfoOffset2)
        for idx in range(0, mtbFrameSetCount):
            stream.readShort()
            mtbFrameSetFrameCounts.append(stream.readShort())
    return mtbFrameSetOffsets, mtbFrameSetFrameCounts

def getFrameParts():
    inc = 1.0 / framePartDiv
    cur = inc
    parts = []
    while cur < 1.0:
        parts.append(cur)
        cur += inc
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
    if(len(frameData) < 0x30 * len(boneNames)):
            print("WARN: Too small frame data, dropping frame %d" % frameIdx)
            return
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
