#include <SDKDDKVer.h>
#define WIN32_LEAN_AND_MEAN
#include <Windows.h>
#include <stdexcept>
#include <iostream>
#include <sstream>
#include <fstream>

class LauncherException : public std::runtime_error {
public:
	LauncherException(const std::string& msg) : runtime_error(msg) {};
};

bool FileExists(const std::string& name) {
	std::ifstream f(name.c_str());
	return f.good();
}

void launchGame() {
	std::string exePath = "game.exe.unpacked.exe";
	std::string dllPath = "animserv.dll";
	if (!FileExists(exePath) || !FileExists(dllPath)) {
		throw LauncherException("game exe or dll does not exist.");
	}

	std::cout << "[*] launching game" << std::endl;
	STARTUPINFOA startupInfo = { 0 };
	PROCESS_INFORMATION	procInfo = { 0 };
	startupInfo.cb = sizeof(startupInfo);
	if (!CreateProcessA(exePath.c_str(), NULL, NULL, NULL, FALSE, CREATE_SUSPENDED, NULL, NULL, &startupInfo, &procInfo)) {
		auto err = GetLastError();
		std::ostringstream ss;
		ss << "failed to launch game with error: " << err;
		auto msg = ss.str();
		throw LauncherException(msg);
	}

	std::cout << "[*] injecting dll" << std::endl;
	auto process = OpenProcess(PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION | PROCESS_VM_OPERATION | PROCESS_VM_WRITE |
		PROCESS_VM_READ | PROCESS_TERMINATE, FALSE, procInfo.dwProcessId);
	if (!process) {
		throw LauncherException("failed to open game process");
	}
	auto dllPathAddr = VirtualAllocEx(process, NULL, 256, MEM_RESERVE | MEM_COMMIT, PAGE_EXECUTE_READWRITE);
	if (!dllPathAddr) {
		throw LauncherException("failed to allocate memory in external process");
	}
	SIZE_T bytesWritten;
	WriteProcessMemory(process, dllPathAddr, dllPath.c_str(), dllPath.length(), &bytesWritten);
	if (!bytesWritten) {
		throw LauncherException("failed to write dll path to external process");
	}

	std::cout << "[*] executing DLL" << std::endl;
	auto loadLibAddr = GetProcAddress(GetModuleHandle("kernel32.dll"), "LoadLibraryA");
	auto thread = CreateRemoteThread(process, NULL, 0, (LPTHREAD_START_ROUTINE)loadLibAddr, dllPathAddr, 0, NULL);
	WaitForSingleObject(thread, INFINITE);
	VirtualFreeEx(process, dllPathAddr, 0, MEM_RELEASE);

	std::cout << "[*] launcher exiting" << std::endl;
	//ResumeThread(procInfo.hThread);
	CloseHandle(procInfo.hProcess);
	CloseHandle(procInfo.hThread);
}

int main() {
	try {
		launchGame();
		return 0;
	}
	catch (const LauncherException& e) {
		std::cout << "[-] error: " << e.what() << std::endl;
		std::cin.get();
	}
	return 1;
}
