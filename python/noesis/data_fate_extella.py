# Noesis loader for Fate/Extella .mdl files
# Supports:
# - Fate/Extella (PC, PS Vita and Switch)
# - Fate/Extella Link (PS Vita)

# ---- Script config ----

onlyLoadAlbedoTexture = True
optimizeDx11Models = False
optimizeGxtModels = False

# -----------------------

from collections import namedtuple
from pprint import pprint
import struct
from os import path

from inc_noesis import *
import noesis
import rapi


def registerNoesisTypes():
    handle = noesis.register("Fate Extella", ".mdl")
    noesis.setHandlerTypeCheck(handle, fateCheckType)
    noesis.setHandlerLoadModel(handle, fateLoadModel)
    if debug:
        noesis.logPopup()
    return 1


def fateCheckType(data):
    if len(data) > 4 and NoeBitStream(data).readInt() == 0x794B504B:
        return 1
    return 0


GprDes = namedtuple('GprDes', 'name unkId unused offset size farOffset farSize unkFlag')
# all *Sid fields are indexes to string bank
Sstv = namedtuple('SSTV', 'texTypeSid texPathSid texNamePrefixSid')
Mate = namedtuple('MATE', 'matId mateNamePrefixSid mateNamePrefix2Sid unkC unk10 unk14 texIdRelated texId')
Prim = namedtuple('PRIM', 'meshId meshGeomNameSid meshNameSid unkC meshGeomName2Sid matId unk18')


def fateLoadModel(data, mdlList):
    dlog("")
    dlog("=== Load .mdl ===")
    rapi.rpgCreateContext()
    bs = NoeBitStream(data)

    bs.seekAbs(4)
    sectCount = bs.readInt()
    bs.readInt()

    sectOffsets = []
    sectSizes = []
    for _ in range(sectCount):
        sectOffsets.append(bs.readInt())
    for _ in range(sectCount):
        sectSizes.append(bs.readInt())

    impl = fateGetPlatformImpl(bs, sectOffsets[0], sectOffsets[1])
    mesh = Mesh(bs, sectOffsets[0], impl)
    brntre = None
    boneSection = None
    if mesh.bonesExist:
        if len(sectOffsets) < 5:
            warn("Mesh has bones info but section describing them does not exist")
        else:
            brntre = Brntre(bs, sectOffsets[3])
            boneSection = BoneSection(bs, sectOffsets[4])
    gpr = Gpr(bs, sectOffsets[1], mesh, boneSection, brntre, impl)

    mdl = rapi.rpgConstructModel()
    mdl.setModelMaterials(NoeModelMaterials(mesh.textures, mesh.materials))
    if boneSection is not None:
        mdl.setBones(boneSection.noeBones)
    mdlList.append(mdl)

    dlog("=== Done ===")
    return 1


def fateGetPlatformImpl(bs, meshOffset, gprOffset):
    bs.seekAbs(meshOffset)
    if bs.readFixedString(4) != "MESH":
        raise ValueError("MESH section expected")
    bs.readBytes(0xC)
    colladaVer = bs.readFixedString(31)
    extellaLinkMode = "Collada Mesh File Version 1.009" in colladaVer

    bs.seekAbs(gprOffset)
    if bs.readString() != "GPR":
        raise ValueError("GPR section expected")
    bs.readBytes(0x4)
    platform = bs.readFixedString(4)
    log("Platform: " + platform)
    if extellaLinkMode:
        log("Mode: Fate/Extella Link")
    else:
        log("Mode: Fate/Extella")
    if platform == "DX11":
        return Dx11Impl()
    elif platform == "GXM":
        return GxmImpl(extellaLinkMode)
    elif platform == "NX":
        return NxImpl()
    else:
        raise ValueError("Unsupported platform: " + platform)


class Mesh:
    def __init__(self, bs, offset, impl):
        bs.seekAbs(offset)
        if bs.readFixedString(4) != "MESH":
            raise ValueError("MESH section expected to be first")
        meshSectSize = bs.readInt()
        meshSectEnd = bs.tell() + meshSectSize
        self.bonesExist = False
        self.boneCount = -1
        self.stringBank = []
        textureInfo = {}
        materialInfo = []
        self.primInfo = []
        self.boneIdToNameMap = {}

        while bs.tell() < meshSectEnd:
            name = bs.readFixedString(4)
            size = bs.readInt()
            end = bs.tell() + size

            if name == "STRB":
                stringCount = bs.readInt()
                if (bs.readFixedString(4)) != "STRL":
                    raise ValueError("STRL expected")
                bs.readInt()
                for _ in range(stringCount):
                    self.stringBank.append(bs.readString())

            elif name == "SAMP":
                texId = bs.readInt()
                textureInfo[texId] = []
                while bs.tell() < end:
                    subName = bs.readFixedString(4)
                    subSize = bs.readInt()
                    subEnd = bs.tell() + subSize
                    if subName == "SSTV":
                        textureInfo[texId].append(Sstv(bs.readInt(), bs.readInt(), bs.readInt()))
                    bs.seek(subEnd)

            elif name == "MATE":
                materialInfo.append(
                    Mate(bs.readInt(), bs.readInt(), bs.readInt(), bs.readInt(), bs.readInt(), bs.readInt(),
                         bs.readInt(), bs.readInt()))

            elif name == "VARI":
                bs.readInt()
                bs.readInt()
                bs.readInt()
                bs.readInt()
                while bs.tell() < end:
                    subName = bs.readFixedString(4)
                    subSize = bs.readInt()
                    subEnd = bs.tell() + subSize
                    if subName == "PRIM":
                        self.primInfo.append(Prim(bs.readInt(), bs.readInt(), bs.readInt(), bs.readInt(), bs.readInt(),
                                                  bs.readInt(), bs.readInt()))
                    bs.seek(subEnd)

            elif name == "BONE":
                self.bonesExist = True
                bs.readInt()
                while bs.tell() < end:
                    subName = bs.readFixedString(4)
                    subSize = bs.readInt()
                    subEnd = bs.tell() + subSize
                    if subName == "BOIF":
                        nameSid = bs.readInt()
                        id = bs.readInt()
                        self.boneIdToNameMap[id] = self.stringBank[nameSid]
                    bs.seek(subEnd)

            bs.seekAbs(end)

        loadedTextures = {}
        self.textures = []
        self.materials = []

        def loadTexture(path, name):
            if path in loadedTextures:
                return loadedTextures[path]
            log("---> load texture: " + texFinalPath)
            try:
                tex = open(path, 'rb').read()
            except IOError:
                log("failed")
                return None
            texture = rapi.loadTexByHandler(tex, impl.getTexHandlerName())
            if texture is None:
                return None
            texture.name = name
            loadedTextures[path] = texture
            self.textures.append(texture)
            return texture

        for mate in materialInfo:
            material = NoeMaterial(self.stringBank[mate.mateNamePrefixSid], "")
            self.materials.append(material)
            if mate.texId == -1:
                continue
            sstvList = textureInfo[mate.texId]
            for sstv in sstvList:
                def getTexPath(extension):
                    texPath = self.stringBank[sstv.texPathSid]
                    texNormalizedName = path.splitext(path.basename(texPath))[0]
                    texFinalPath = rapi.getDirForFilePath(rapi.getInputName()) + texNormalizedName + extension
                    if rapi.checkFileExists(texFinalPath):  # check in current dir first
                        dlog("Tex in current: " + texFinalPath)
                        return texFinalPath, texNormalizedName

                    # resource/target/win
                    baseResFolder = path.dirname(path.abspath(rapi.getDirForFilePath(rapi.getInputName())))
                    texFinalPath = path.join(path.join(baseResFolder, "mdltex"), texNormalizedName) + extension
                    if rapi.checkFileExists(texFinalPath):
                        dlog("Tex from ../mdltex: " + texFinalPath)
                        return texFinalPath, texNormalizedName
                    dlog("Tex not found: " + texFinalPath)
                    return None, None

                texFinalPath, texNormalizedName = getTexPath(impl.getTexExtensions()[0])
                if texFinalPath is None:
                    texFinalPath, texNormalizedName = getTexPath(impl.getTexExtensions()[1])
                if texFinalPath is None:
                    continue

                # if "skin" in texNormalizedName or "hair" in texNormalizedName:
                material.setDefaultBlend(0)

                if skipTextures:
                    continue

                texType = self.stringBank[sstv.texTypeSid]
                # always ignored types for Extella: Outline0, Custom0
                # always ignored types for Extella Link: FogIBL, ARMMap, ENVBRDF_LUT, EmissiveMap
                if texType in ["Albedo0", "DiffuseMap"]:
                    loadTexture(texFinalPath, texNormalizedName)
                    material.setTexture(texNormalizedName)
                if not onlyLoadAlbedoTexture:
                    if texType in ["Normal0", "NormalMap"]:
                        loadTexture(texFinalPath, texNormalizedName)
                        material.setNormalTexture(texNormalizedName)
                    if texType == "Speculer0":  # not a typo
                        loadTexture(texFinalPath, texNormalizedName)
                        material.setSpecularTexture(texNormalizedName)
                    if texType == "envTexture":
                        loadTexture(texFinalPath, texNormalizedName)
                        material.setEnvTexture(texNormalizedName)


class Gpr:
    def __init__(self, bs, offset, mesh, boneSect, brntre, impl):
        bs.seekAbs(offset)
        if bs.readString() != "GPR":
            raise ValueError("GPR section expected to be second")

        # calculate offset for far section
        bs.readBytes(0xC)
        self.farOffset = bs.tell()
        bs.readBytes(0x18)
        self.farOffset += bs.readInt()

        # go into GPR descriptors
        bs.seekAbs(offset + 0x40)

        # read HEAP
        if bs.readFixedString(4) != "HEAP":
            raise ValueError("HEAP data expected")
        bs.readInt()
        bs.readInt()
        gprDesSize = bs.readInt()
        bs.readInt()
        heapSize = bs.readInt()
        bs.readInt()
        gprDesCount = bs.readInt()

        self.descriptors = []
        for _ in range(gprDesCount):
            self.descriptors.append(GprDes(bs.readFixedString(4), bs.readInt(), bs.readInt(), bs.readInt(),
                                           bs.readInt(), bs.readInt(), bs.readInt(), bs.readInt()))

        bs.readBytes(heapSize)
        modelName = bs.readString()
        log("Model name: " + modelName)
        bs.seekAlign(0x10)

        self.nearOffset = bs.tell()

        dlog("GPR near offset: " + hex(self.nearOffset))
        dlog("GPR far offset: " + hex(self.farOffset))

        # Collect IXBF, VXBF, VXST
        ixbfList = []
        vxbfList = []
        vxstList = []
        for _, des in enumerate(self.descriptors):
            if des.name == "IXBF":
                ixbfList.append(des)
            if des.name == "VXBF":
                vxbfList.append(des)
            if des.name == "VXST":
                vxstList.append(des)

        vxstEntryCount = []
        for _, des in enumerate(vxstList):
            bs.seek(des.offset + self.nearOffset, NOESEEK_ABS)
            bs.readBytes(impl.getVxstEntryCountOffset())
            vxstEntryCount.append(bs.readInt())

        vxbfEntryCount = []
        vxbfEntryLen = []
        for _, des in enumerate(vxbfList):
            bs.seek(des.offset + self.nearOffset, NOESEEK_ABS)
            bs.readBytes(0x8)
            vxbfEntryCount.append(bs.readInt())
            vxbfEntryLen.append(bs.readInt())

        # ddump(vxstEntryCount)
        # ddump(vxbfEntryCount)
        # ddump(vxbfEntryLen)

        if len(vxbfList) > len(ixbfList):
            warn("VXBF count > IXBF count, some data not processed")
            # not sure if this will work for Link
            idxToRemove = []
            for i, length in enumerate(vxbfEntryLen):
                if length <= 16:
                    idxToRemove.append(i)
            for idx in sorted(idxToRemove, reverse=True):
                del vxbfList[idx]
                del vxbfEntryCount[idx]
                del vxbfEntryLen[idx]
        elif len(vxbfList) < len(ixbfList):
            warn("VXBF count < IXBF count, some data not processed")

        for i, des in enumerate(vxbfList):
            rapi.rpgSetName(mesh.stringBank[mesh.primInfo[i].meshGeomNameSid])
            bs.seek(des.farOffset + self.farOffset, NOESEEK_ABS)
            vertStride = vxbfEntryLen[i]
            dlog("Parse verts at " + hex(bs.tell()) + " with stride " + str(vertStride))
            vertBuff = bs.readBytes(des.farSize)

            def bindBones(boneOffset, weightOffset, stride):
                VertexBones(boneOffset, weightOffset, stride)

            class VertexBones:
                def __init__(self, boneOffset, weightOffset, stride):
                    if boneSect is None:
                        return

                    self.boneOffset = boneOffset
                    self.weightOffset = weightOffset
                    self.stride = stride
                    self.boneData = []
                    self.weightData = []

                    self.collectBones()
                    self.collectWeights()
                    self.mapBones()
                    flatten = lambda l: [item for sublist in l for item in sublist]
                    boneBuf = struct.pack("B" * (vxbfEntryCount[i] * 4), *flatten(self.boneData))
                    rapi.rpgBindBoneIndexBuffer(boneBuf, noesis.RPGEODATA_BYTE, 0x4, 4)
                    weightBuf = struct.pack("f" * (vxbfEntryCount[i] * 4), *flatten(self.weightData))
                    rapi.rpgBindBoneWeightBuffer(weightBuf, noesis.RPGEODATA_FLOAT, 0x10, 4)

                def collectBones(self):
                    bsi = NoeBitStream(vertBuff)
                    for _ in range(vxbfEntryCount[i]):
                        bsi.readBytes(self.boneOffset)
                        b1 = bsi.readUByte()
                        b2 = bsi.readUByte()
                        b3 = bsi.readUByte()
                        b4 = bsi.readUByte()
                        bsi.readBytes(self.stride - self.boneOffset - 0x4)
                        self.boneData.append([b1, b2, b3, b4])

                def collectWeights(self):
                    wsi = NoeBitStream(vertBuff)
                    for _ in range(vxbfEntryCount[i]):
                        wsi.readBytes(self.weightOffset)
                        w1 = wsi.readUByte() / 255.0
                        w2 = wsi.readUByte() / 255.0
                        w3 = wsi.readUByte() / 255.0
                        w4 = wsi.readUByte() / 255.0
                        wsi.readBytes(self.stride - self.weightOffset - 0x4)
                        self.weightData.append((w1, w2, w3, w4))

                def mapBones(self):
                    for b, w in zip(self.boneData, self.weightData):
                        for i, (bv, wv) in enumerate(zip(b, w)):
                            if wv != 0:
                                try:
                                    b[i] = brntre.boneMeshToSkelMap[bv]
                                except KeyError:
                                    #warn("Missing mapping for mesh bone " + str(bv))
                                    pass

            impl.bindVertStride(modelName, rapi, des, vertBuff, vertStride, bindBones)
            bs.seek(ixbfList[i].farOffset + self.farOffset, NOESEEK_ABS)

            faceList = impl.getFaceList(bs, vxstEntryCount[i])
            faceBuff = struct.pack('H' * len(faceList), *faceList)

            rapi.rpgSetMaterial(mesh.materials[mesh.primInfo[i].matId].name)
            if impl.needsUVFlip():
                rapi.rpgSetUVScaleBias(NoeVec3((1.0, -1.0, 1.0)), NoeVec3((1.0, 1.0, 1.0)))
            rapi.rpgCommitTriangles(faceBuff, noesis.RPGEODATA_USHORT, len(faceList), noesis.RPGEO_TRIANGLE, 1)
            if impl.needsOptimize():
                rapi.rpgOptimize()
            rapi.rpgClearBufferBinds()


class Brntre:
    def __init__(self, bs, offset):
        bs.seekAbs(offset)
        if bs.readFixedString(16) != "BRNTREx86Ver2.00":
            raise ValueError("Expected section to start with 'BRNTREx86Ver2.00'")
        boneCount = bs.readInt()
        meshMappedBoneCount = bs.readInt()
        bs.readInt()
        bs.readInt()
        self.boneMeshToSkelMap = {}
        for _ in range(boneCount):
            bs.readInt()
            name = bs.readFixedString(0x10)
            id = bs.readShort()
            bs.readShort()
            bs.readShort()
            meshId = bs.readShort()
            bs.readBytes(0x3C)
            if meshId != -1:
                self.boneMeshToSkelMap[meshId] = id


class BoneSection:
    def __init__(self, bs, offset):
        bs.seekAbs(offset)
        if bs.readFixedString(4) != "60SE":
            raise ValueError("Expected bone section to start with '60SE'")
        bs.readBytes(0xC)

        boneCount = bs.readInt()
        bs.readInt()
        matrixOffset = bs.tell() + bs.readInt()

        bs.readBytes(0x14)

        boneNamesHeaderOffset = bs.tell() + bs.readInt()
        bs.readInt()
        bs.readInt()

        parentMap = {}
        parentRelCount = bs.readInt()
        bs.readInt()
        bs.readInt()
        for _ in range(parentRelCount):
            child = bs.readUByte()
            bs.readUByte()
            parent = bs.readUByte()
            bs.readUByte()
            if child != parent:
                parentMap[child] = parent

        boneNames = []
        bs.seekAbs(boneNamesHeaderOffset)
        bs.readBytes(0x1C)
        bs.seekAbs(bs.tell() + bs.readInt())
        for _ in range(boneCount):
            boneNames.append(bs.readString())

        boneMatrixes = []
        bs.seekAbs(matrixOffset)
        for _ in range(boneCount):
            qx, qy, qz, qw = bs.readFloat(), bs.readFloat(), bs.readFloat(), bs.readFloat()
            x, y, z = bs.readFloat(), bs.readFloat(), bs.readFloat()
            bs.readBytes(0x14)
            quat = NoeQuat((qx, qy, qz, qw))
            mat = quat.toMat43(transposed=1)
            mat[3] = NoeVec3((x, y, z))
            boneMatrixes.append(mat)

        self.noeBones = []
        for i in range(boneCount):
            self.noeBones.append(NoeBone(i, boneNames[i], boneMatrixes[i], None, parentMap.get(i, -1)))
        self.noeBones = rapi.multiplyBones(self.noeBones)


class GxmImpl:
    def __init__(self, extellaLinkMode):
        self.extellaLinkMode = extellaLinkMode

    def bindVertStride(self, modelName, rapi, des, vertBuff, vertStride, bindBones):
        if vertStride == 19 and not self.extellaLinkMode:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xF)
        elif vertStride == 20:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            if self.extellaLinkMode:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x10)
            else:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xC)
        elif vertStride == 23:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            if self.extellaLinkMode:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x13)
            else:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xF)
        elif vertStride == 24:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            if self.extellaLinkMode:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x14)
            else:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xC)
        elif vertStride == 27:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x17)
        elif vertStride == 28:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            if self.extellaLinkMode:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
            else:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xC)
        elif vertStride == 31:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            if self.extellaLinkMode:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x13)
                bindBones(0x1B, 0x17, vertStride)
            else:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xF)
                #bindBones(0x17, 0x1B, vertStride)
        elif vertStride == 35:  #
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            if self.extellaLinkMode:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x17)
                bindBones(0x1F, 0x1B, vertStride)
            else:
                rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0xF)
                #bindBones(0x1F, 0x1B, vertStride)
        else:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            warn("Don't know how to parse vertex stride: {}.".format(vertStride))

    def getFaceList(self, bs, vxstEntryCount):
        def nextFace():
            nextFace.idx += 1
            return bs.readUShort()

        nextFace.idx = 0
        faceList = []

        while nextFace.idx < vxstEntryCount:
            f1 = nextFace()
            f2 = nextFace()
            f3 = nextFace()
            faceList.extend([f1, f2, f3])
        return faceList

    def getVxstEntryCountOffset(self):
        return 0x10

    def getTexExtensions(self):
        return [".mxt", ".gxt"]

    def getTexHandlerName(self):
        return ".gxt"

    def needsUVFlip(self):
        return self.extellaLinkMode

    def needsOptimize(self):
        return optimizeGxtModels


class Dx11Impl:
    def bindVertStride(self, modelName, rapi, des, vertBuff, vertStride, bindBones):
        useAltStride44OrderFor = []
        if modelName == "SV1310_PS4":
            useAltStride44OrderFor.append(0x47a70)
        if modelName == "SV0803_PS4":
            useAltStride44OrderFor.append(0x592c0)

        # not implemented:
        # 4, 8, 12 - do not have IXBF
        # 16 - unknown format, do not have IXBF probably, 20 - not used by any game model
        if vertStride == 24:  # 0xC, 0x10 unk floats
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x14)
        elif vertStride == 28:  # 0xC, 0x10, 0x14 unk floats
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
        elif vertStride == 32:  # 0xC, 0x10, 0x14 unk floats, 0x1C unk
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
        elif vertStride == 36:  # 0xC, 0x10, 0x14 unk floats, 0x1C unk
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
            rapi.rpgBindColorBufferOfs(vertBuff, noesis.RPGEODATA_UBYTE, vertStride, 0x20, 4)
        elif vertStride == 40:  # 0xC, 0x10, 0x14 unk floats, 0x1C unk
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
            bindBones(0x20, 0x24, vertStride)
        elif vertStride == 44:  # 0xC, 0x10, 0x14 unk floats, 0x1C unk
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
            if des.farOffset in useAltStride44OrderFor:
                bindBones(0x24, 0x28, vertStride)
            else:
                bindBones(0x20, 0x24, vertStride)
                rapi.rpgBindColorBufferOfs(vertBuff, noesis.RPGEODATA_UBYTE, vertStride, 0x28, 4)
        elif vertStride == 48:  # 0xC, 0x10, 0x14 unk floats, 0x1C, 0x1C, 0x20, 0x24, 0x28 unk
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            rapi.rpgBindUV1BufferOfs(vertBuff, noesis.RPGEODATA_HALFFLOAT, vertStride, 0x18)
            rapi.rpgBindColorBufferOfs(vertBuff, noesis.RPGEODATA_UBYTE, vertStride, 0x2C, 4)
        else:
            rapi.rpgBindPositionBuffer(vertBuff, noesis.RPGEODATA_FLOAT, vertStride)
            warn("Don't know how to parse vertex stride: {}.".format(vertStride))

    def getFaceList(self, bs, vxstEntryCount):
        def nextFace():
            nextFace.idx += 1
            return bs.readUShort()

        nextFace.idx = 0
        faceFlip = True
        faceList = []

        f1 = nextFace()
        f2 = nextFace()

        while nextFace.idx < vxstEntryCount:
            f3 = nextFace()
            if f3 == 0xFFFF:
                f1 = nextFace()
                f2 = nextFace()
                faceFlip = True
            else:
                faceFlip = not faceFlip
                if f1 != f2 and f2 != f3 and f3 != f1:
                    if not faceFlip:
                        faceList.extend([f1, f2, f3])
                    else:
                        faceList.extend([f1, f3, f2])
                f1 = f2
                f2 = f3
        return faceList

    def getVxstEntryCountOffset(self):
        return 0x18

    def getTexExtensions(self):
        return [".mds", ".dds"]

    def getTexHandlerName(self):
        return ".dds"

    def needsUVFlip(self):
        return False

    def needsOptimize(self):
        return optimizeDx11Models


class NxImpl(Dx11Impl):
    def getTexExtensions(self):
        return [".mntx", ".bntx"]

    def getTexHandlerName(self):
        return ".bntx"

# Helpers

def dlog(msg):
    if debug:
        log(msg)


def ddump(obj):
    if debug:
        pprint(obj)


def log(msg):
    noesis.logOutput(msg + "\n")


def warn(msg):
    noesis.logOutput("WARNING: " + msg + "\n")
    noesis.logPopup()


def error(msg):
    noesis.logError("ERROR: " + msg + "\n")
    noesis.logPopup()


def __readFixedString(self, len):
    return self.readBytes(len).decode("ASCII").rstrip("\0")


def __seekAlign(self, pad):
    if self.tell() % pad == 0:
        return
    absOffset = (self.tell() // pad + 1) * pad
    self.seek(absOffset, NOESEEK_ABS)


def __seekAbs(self, offset):
    self.seek(offset, NOESEEK_ABS)


NoeBitStream.readFixedString = __readFixedString
NoeBitStream.seekAlign = __seekAlign
NoeBitStream.seekAbs = __seekAbs

debug = False
skipTextures = debug
