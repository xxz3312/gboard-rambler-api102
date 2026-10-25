"""Check module entry and callback references in the actual shrunk APK DEX."""
import struct
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as apk:
    entries=[s.strip() for s in apk.read("META-INF/xposed/java_init.list").decode().splitlines() if s.strip() and not s.startswith("#")]
    assert entries==["dev.kaede.pixelboard.PixelBoardModule"], entries
    classes=set()
    methods=set()
    for name in apk.namelist():
        if not name.endswith(".dex"): continue
        data=apk.read(name)
        u32=lambda p: struct.unpack_from("<I",data,p)[0]
        u16=lambda p: struct.unpack_from("<H",data,p)[0]
        strings=[]
        for index in range(u32(56)):
            pos=u32(u32(60)+index*4)
            while data[pos]&128: pos+=1
            pos+=1
            strings.append(data[pos:data.index(b"\x00",pos)].decode("utf-8","replace"))
        types=[strings[u32(u32(68)+i*4)] for i in range(u32(64))]
        for index in range(u32(96)):
            classes.add(types[u32(u32(100)+index*32)])
        for index in range(u32(88)):
            pos=u32(92)+index*8
            methods.add((types[u16(pos)],strings[u32(pos+4)]))
    entry="Ldev/kaede/pixelboard/PixelBoardModule;"
    assert entry in classes
    for method in ("onModuleLoaded","onPackageReady"):
        assert (entry,method) in methods,method
    for cls in ("ModuleApp","SettingsActivity","CloudRuntimeStatus","GboardResolver","FeatureConfig","ModuleRelease","ModuleUpdateChecker","RamblerDictationFilter"):
        assert "Ldev/kaede/pixelboard/"+cls+";" in classes,cls
print("Shrunk APK entry, callbacks and configuration/status classes verified.")
