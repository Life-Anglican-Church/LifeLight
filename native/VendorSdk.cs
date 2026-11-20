using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;

static class VendorFiles {
    public static string Hash(byte[] data){using(var h=System.Security.Cryptography.SHA256.Create())return BitConverter.ToString(h.ComputeHash(data)).Replace("-","").ToLowerInvariant();}
    public const string SdkHash="2c45136f7833f4096585ee319037d29cef6e91e5c0146e4d9c4410bcd1219b1b";
    public const string UsbHash="00caca07869b19d10b370552ac7cc2f6f2ee246fc15db11650f6cd3f4ef9b666";
    public static string Folder {get{return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"LifeAC-Lighting","vendor");}}
    public static bool Ready(){return Check(Path.Combine(Folder,"DevKit.dll"),SdkHash)&&Check(Path.Combine(Folder,"libusb0.dll"),UsbHash);}
    static bool Check(string path,string hash){return File.Exists(path)&&Hash(File.ReadAllBytes(path))==hash;}
    public static void FromZip(string path){
        byte[] sdk=null,usb=null;using(var zip=ZipFile.OpenRead(path))foreach(var e in zip.Entries){string n=e.FullName.Replace('\\','/');
            if(n.EndsWith("/Lib/Windows 32bit/DevKit.dll",StringComparison.OrdinalIgnoreCase))sdk=Read(e,30000000);
            if(n.EndsWith("/Drivers/Windows/x86/libusb0_x86.dll",StringComparison.OrdinalIgnoreCase))usb=Read(e,2000000);
        }
        if(sdk==null||usb==null||Hash(sdk)!=SdkHash||Hash(usb)!=UsbHash)throw new InvalidDataException("SDK fingerprint mismatch");
        Directory.CreateDirectory(Folder);File.WriteAllBytes(Path.Combine(Folder,"DevKit.dll"),sdk);File.WriteAllBytes(Path.Combine(Folder,"libusb0.dll"),usb);
    }
    static byte[] Read(ZipArchiveEntry e,int max){if(e.Length>max)throw new InvalidDataException();using(var r=e.Open())using(var m=new MemoryStream()){r.CopyTo(m);if(m.Length>max)throw new InvalidDataException();return m.ToArray();}}
}
sealed class NativeSdk {
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)]static extern IntPtr LoadLibraryEx(string path,IntPtr file,uint flags);
    [DllImport("kernel32.dll",CharSet=CharSet.Ansi,ExactSpelling=true)]static extern IntPtr GetProcAddress(IntPtr module,string name);
    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]delegate IntPtr Factory();
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)]delegate int CountFn(IntPtr self);
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)]delegate int IntFn(IntPtr self,int device);
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)]delegate void VoidFn(IntPtr self);
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)][return:MarshalAs(UnmanagedType.I1)]delegate bool BoolFn(IntPtr self,int device);
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)][return:MarshalAs(UnmanagedType.I1)]delegate bool OpenFn(IntPtr self,int device,int mode,IntPtr key,int length);
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)][return:MarshalAs(UnmanagedType.I1)]delegate bool DataFn(IntPtr self,int device,IntPtr buffer);
    [UnmanagedFunctionPointer(CallingConvention.ThisCall)][return:MarshalAs(UnmanagedType.I1)]delegate bool FrameFn(IntPtr self,int device,int universe,IntPtr buffer);
    IntPtr dll,self;CountFn count;IntFn universes;BoolFn connected;OpenFn open;VoidFn init,close;
    DataFn firmware,timing;FrameFn read,write;
    T Bind<T>(string name)where T:class {var p=GetProcAddress(dll,name);if(p==IntPtr.Zero)throw new EntryPointNotFoundException(name);return Marshal.GetDelegateForFunctionPointer(p,typeof(T))as T;}
    public NativeSdk(string folder){
        if(IntPtr.Size!=4)throw new PlatformNotSupportedException("x86 required");
        if(!VendorFiles.Ready() || Path.GetFullPath(folder)!=Path.GetFullPath(VendorFiles.Folder))throw new InvalidDataException("Unverified vendor files");
        // Search dependencies only next to the pinned DLL and in Windows system directories.
        dll=LoadLibraryEx(Path.Combine(folder,"DevKit.dll"),IntPtr.Zero,0x100|0x800);if(dll==IntPtr.Zero)throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());
        count=Bind<CountFn>("_ZN6DevKit21getNumberOfUsbDevicesEv");universes=Bind<IntFn>("_ZN6DevKit20getNumberOfUniversesEi");connected=Bind<BoolFn>("_ZN6DevKit17isDeviceConnectedEi");
        open=Bind<OpenFn>("_ZN6DevKit13openUsbDeviceEi13cqInOutConfigPhi");init=Bind<VoidFn>("_ZN6DevKit11initDevicesEv");close=Bind<VoidFn>("_ZN6DevKit15closeAllDevicesEv");
        firmware=Bind<DataFn>("_ZN6DevKit19readFirmwareVersionEiR17cqFirmwareVersion");timing=Bind<DataFn>("_ZN6DevKit13readDmxConfigEiR11cqDmxConfig");
        read=Bind<FrameFn>("_ZN6DevKit15readDmxUniverseEiiPh");write=Bind<FrameFn>("_ZN6DevKit16writeDmxUniverseEiiPh");
        self=Bind<Factory>("createDevKitClass")();if(self==IntPtr.Zero)throw new InvalidOperationException("Factory failed");
    }
    public int Count(){return count(self);}public bool Open(int id,int mode){bool ok=open(self,id,mode,IntPtr.Zero,0);if(ok)init(self);return ok;}
    public void Close(){close(self);}public int Universes(int id){return universes(self,id);}public bool Connected(int id){return connected(self,id);}
    public byte[] ReadData(int id,bool fw){var p=Marshal.AllocHGlobal(4);try{Marshal.Copy(new byte[4],0,p,4);if(!(fw?firmware(self,id,p):timing(self,id,p)))return null;var b=new byte[4];Marshal.Copy(p,b,0,4);return b;}finally{Marshal.FreeHGlobal(p);}}
    public byte[] ReadInput(int id,int u){var p=Marshal.AllocHGlobal(512);try{Marshal.Copy(new byte[512],0,p,512);if(!read(self,id,u,p))return null;var b=new byte[512];Marshal.Copy(p,b,0,512);return b;}finally{Marshal.FreeHGlobal(p);}}
    public bool Write(int id,int u,byte[] frame){if(frame.Length!=512)throw new ArgumentException();var p=Marshal.AllocHGlobal(512);try{Marshal.Copy(frame,0,p,512);return write(self,id,u,p);}finally{Marshal.FreeHGlobal(p);}}
}
