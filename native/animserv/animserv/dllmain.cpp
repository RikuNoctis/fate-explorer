#include <SDKDDKVer.h>
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <sstream>
#include <fstream>
#include <vector>
#include "xxhash.h"

#include <winsock2.h>
#include <ws2tcpip.h>
#include <iterator>
#pragma comment (lib, "Ws2_32.lib")

typedef uint8_t u8;
typedef uint16_t u16;
typedef uint32_t u32;
typedef uint64_t u64;
typedef int8_t i8;
typedef int16_t i16;
typedef int32_t i32;
typedef int64_t i64;

constexpr i32 DEFAULT_BUFLEN = 1024;
constexpr const char* DEFAULT_PORT = "54217";

typedef u64(__fastcall *AnimDecoder)(void* decodedDataOut, void* unkOut, void* animHeader,
	void* frameSetData, i32 frameCount, i32 frameIdx, float framePart);

#pragma pack(push,1)
struct BasePacket {
	const i16 id;
	BasePacket() : id(0x0) {}
};

struct LoadFileRequest {
	const i16 id;
	wchar_t path[256];
	LoadFileRequest() : id(0x3330) {}
};

struct LoadFileResponse {
	const i16 id;
	i16 error;
	LoadFileResponse() : id(0x3331) {}
};

struct DecodeFrameRequest {
	const i16 id;
	i32 framesetDefOffset;
	i32 frameCount;
	i32 frameIdx;
	float framePart;
	DecodeFrameRequest() : id(0x3332) {}
};

struct DecodeFrameResponse {
	const i16 id;
	i16 error;
	i32 dataSize;
	// decodedData[dataSize] follows...
	DecodeFrameResponse() : id(0x3333) {}
};
#pragma pack(pop)

class SocketStreamException : public std::runtime_error {
public:
	SocketStreamException(const std::string& msg, bool cleanDisconnect) : runtime_error(msg), cleanDisconnect(cleanDisconnect) {};
	const bool cleanDisconnect;
};

class SocketStream
{
public:
	SocketStream(SOCKET socket) : m_socket(socket) {}

	i8 readByte() {
		if (m_bufPos >= m_bufCapcity) {
			m_bufPos = 0;
			fillBuf();
		}
		i8 value = m_buf[m_bufPos];
		m_bufPos++;
		return value;
	}

	i16 readShort() {
		i8 b1 = readByte();
		i8 b2 = readByte();
		return (b2 & 0xFF) << 8 | (b1 & 0xFF);
	}

	i32 readInt() {
		i8 b1 = readByte();
		i8 b2 = readByte();
		i8 b3 = readByte();
		i8 b4 = readByte();
		return (b4 & 0xFF) << 24 | (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b1 & 0xFF);
	}

	float readFloat() {
		i32 data = readInt();
		return *reinterpret_cast<float*>(&data);
	}

	void readFully(u8 buf[], i32 bufSize) {
		for (int i = 0; i < bufSize; i++) {
			buf[i] = readByte();
		}
	}

	void write(const void* buf, i32 bufSize) {
		int result = send(m_socket, (char*)buf, bufSize, 0);
		if (result == SOCKET_ERROR) {
			throw SocketStreamException("send failed", false);
		}
	}
private:
	SOCKET m_socket;
	u8 m_buf[DEFAULT_BUFLEN];
	i32 m_bufCapcity = 0;
	i32 m_bufPos = 0;

	void fillBuf() {
		m_bufCapcity = recv(m_socket, (char*)m_buf, DEFAULT_BUFLEN, 0);
		if (m_bufCapcity == 0) {
			throw SocketStreamException("connection closed", true);
		} else if (m_bufCapcity < 0) {
			throw SocketStreamException("recv failed", false);
		}
	}
};

class RemoteClient {
public:
	RemoteClient(AnimDecoder animDecoder, u32 connId, SOCKET socket) : m_animDecoder(animDecoder),
		m_connectionId(connId), m_clientSocket(socket), m_socketStream(socket) {
		log("accepted connection");
		const HANDLE thread = CreateThread(NULL, 0, RemoteClientThreadStart, this, 0, NULL);
		if (thread) {
			CloseHandle(thread);
		}
	}

private:
	const AnimDecoder m_animDecoder;
	const u32 m_connectionId;
	const SOCKET m_clientSocket;
	std::vector<u8> m_currentFileBuf;
	SocketStream m_socketStream;

	static DWORD WINAPI RemoteClientThreadStart(LPVOID lpParameter) {
		RemoteClient* client = static_cast<RemoteClient*>(lpParameter);
		client->recvData();
		delete client;
		return 0;
	}

	void recvData() {
		try {
			while (true) {
				i16 packetId = m_socketStream.readShort();
				handlePacket(packetId);
			}
		}
		catch (const SocketStreamException& e) {
			if (e.cleanDisconnect) {
				log(e.what());
			} else {
				elog("error: ", e.what(), " ", WSAGetLastError());
				closesocket(m_clientSocket);
			}
		}
		catch (const std::runtime_error& e) {
			elog("error: ", e.what());
			closesocket(m_clientSocket);
		}
	}

	void handlePacket(i16 packetId) {
		switch (packetId) {
		case 0x3330: {
			u8 pathBytes[256];
			m_socketStream.readFully(pathBytes, 256);
			wchar_t* path = (wchar_t*)pathBytes;
			LoadFileResponse response;
			wcharLog("load file: ", path);
			std::ifstream file(path, std::ios::binary);
			if (file.good() == false) {
				response.error = 1;
				sendResponse(&response, sizeof(response));
				return;
			}
			file.unsetf(std::ios::skipws);
			file.seekg(0, std::ios::end);
			std::streamsize size = file.tellg();
			file.seekg(0, std::ios::beg);
			m_currentFileBuf.clear();
			m_currentFileBuf.reserve(size);
			m_currentFileBuf.insert(m_currentFileBuf.begin(),
				std::istream_iterator<u8>(file),
				std::istream_iterator<u8>());
			response.error = 0;
			sendResponse(&response, sizeof(response));
			break;
		}
		case 0x3332: {
			const i32 framesetDefOffset = m_socketStream.readInt();
			const i32 frameCount = m_socketStream.readInt();
			const i32 frameIdx = m_socketStream.readInt();
			const float framePart = m_socketStream.readFloat();
			DecodeFrameResponse response;
			if (m_currentFileBuf.size() < 0x40) {
				response.error = 1;
				response.dataSize = 0;
				elog("decode failed, loaded file invalid");
				sendResponse(&response, sizeof(response));
				return;
			}
			if (framesetDefOffset > m_currentFileBuf.size()) {
				response.error = 2;
				response.dataSize = 0;
				elog("decode failed, specified frameset out of bounds");
				sendResponse(&response, sizeof(response));
				return;
			}
			if (frameIdx > frameCount) {
				response.error = 3;
				response.dataSize = 0;
				elog("decode failed, frame index out of bounds");
				sendResponse(&response, sizeof(response));
				return;
			}

			u16 boneCount = m_currentFileBuf[0xE] | (m_currentFileBuf[0xF] << 8);
			u32 memoryNeeded = boneCount * 0x30 + 0x30;
			u8* decodedData = new u8[memoryNeeded];
			log("decode frame ", frameIdx, ", frame part ", framePart);
			m_animDecoder(decodedData, nullptr, m_currentFileBuf.data(), m_currentFileBuf.data() + framesetDefOffset, frameCount,
				frameIdx, framePart);
			response.error = 0;
			response.dataSize = memoryNeeded;
			sendResponse(&response, sizeof(response), decodedData, memoryNeeded);
			delete[] decodedData;
			break;
		}
		default:
			elog("unsupported packet id ", packetId);
			throw std::runtime_error("unsupported packet type");
		}
	}

	void sendResponse(const void* data, i32 dataLen) {
		return sendResponse(data, dataLen, nullptr, 0);
	}

	void sendResponse(const void* responseData, i32 responseLen, const void* dataTail, i32 dataTailSize) {
		m_socketStream.write(responseData, responseLen);
		if(dataTail != nullptr) {
			m_socketStream.write(dataTail, dataTailSize);
		}
	}

	template <typename Arg, typename... Args>
	void log(Arg&& arg, Args&&... args) {
		std::stringstream msgOut;
		msgOut << "[*] [" << m_connectionId << "] ";
		msgOut << std::forward<Arg>(arg);
		using expander = int[];
		(void)expander {
			0, (void(msgOut << std::forward<Args>(args)), 0)...
		};
		msgOut << std::endl;
		auto str = msgOut.str();
		std::cout << str;
	}

	template <typename Arg, typename... Args>
	void wcharLog(Arg&& arg, Args&&... args) {
		std::wstringstream msgOut;
		msgOut << "[*] [" << m_connectionId << "] ";
		msgOut << std::forward<Arg>(arg);
		using expander = int[];
		(void)expander {
			0, (void(msgOut << std::forward<Args>(args)), 0)...
		};
		msgOut << std::endl;
		auto str = msgOut.str();
		std::wcout << str;
	}

	template <typename Arg, typename... Args>
	void elog(Arg&& arg, Args&&... args) {
		std::stringstream msgOut;
		msgOut << "[-] [" << m_connectionId << "] ";
		msgOut << std::forward<Arg>(arg);
		using expander = int[];
		(void)expander {
			0, (void(msgOut << std::forward<Args>(args)), 0)...
		};
		msgOut << std::endl;
		auto str = msgOut.str();
		std::cout << str;
	}
};

u32 startServerSocket(AnimDecoder animDecoder) {
	i32 iResult;
	struct addrinfo *result = NULL;

	WSADATA wsaData;
	iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
	if (iResult != 0) {
		printf("[-] socket WSAStartup failed with error: %d\n", iResult);
		return 1;
	}

	struct addrinfo hints;
	ZeroMemory(&hints, sizeof(hints));
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;
	hints.ai_flags = AI_PASSIVE;

	iResult = getaddrinfo(NULL, DEFAULT_PORT, &hints, &result);
	if (iResult != 0) {
		printf("[-] socket getaddrinfo failed with error: %d\n", iResult);
		WSACleanup();
		return 1;
	}

	SOCKET listenSocket = socket(result->ai_family, result->ai_socktype, result->ai_protocol);
	if (listenSocket == INVALID_SOCKET) {
		printf("[-] socket failed with error: %ld\n", WSAGetLastError());
		freeaddrinfo(result);
		WSACleanup();
		return 1;
	}

	iResult = bind(listenSocket, result->ai_addr, (int)result->ai_addrlen);
	if (iResult == SOCKET_ERROR) {
		printf("[-] socket bind failed with error: %d\n", WSAGetLastError());
		freeaddrinfo(result);
		closesocket(listenSocket);
		WSACleanup();
		return 1;
	}
	freeaddrinfo(result);

	iResult = listen(listenSocket, SOMAXCONN);
	if (iResult == SOCKET_ERROR) {
		printf("[-] socket listen failed with error: %d\n", WSAGetLastError());
		closesocket(listenSocket);
		WSACleanup();
		return 1;
	}

	printf("[*] server started using port %s\n", DEFAULT_PORT);
	u32 connectionId = 0;
	while (true) {
		SOCKET clientSocket = accept(listenSocket, NULL, NULL);
		if (clientSocket == INVALID_SOCKET) {
			printf("[-] client accept failed with error: %d\n", WSAGetLastError());
			closesocket(listenSocket);
			WSACleanup();
			return 1;
		}

		new RemoteClient(animDecoder, connectionId++, clientSocket);
	}
}

void spawnConsole() {
	AllocConsole();
	freopen_s((FILE**)stdout, "CONOUT$", "w", stdout);
	freopen_s((FILE**)stdin, "CONIN$", "r", stdin);
	std::cout << "[*] animserv starting" << std::endl;
}

void waitForDbg() {
	std::cout << "[*] waiting for debugger to attach" << std::endl;
	while (!IsDebuggerPresent()) {
		Sleep(100);
	}
}

AnimDecoder findAnimDecoder() {
	u64 exeBaseLoad = (u64)GetModuleHandle(NULL);
	u64 funcSig = 0x4528dcfcf9ecf90;
	AnimDecoder animDecoder = nullptr;
	for (u64 offset = 0; offset < 0x500000; offset += 0x10) {
		u64 sig = XXH64((void*)(exeBaseLoad + offset), 0x40, 0);
		if (funcSig == sig) {
			std::cout << "[*] found animation decoder at 0x" << std::hex << offset << std::endl;
			animDecoder = (AnimDecoder)(exeBaseLoad + offset);
			break;
		}
	}
	if (animDecoder == nullptr) {
		std::cout << "[-] error: could not found animation decoder in exe" << std::endl;
		std::cin.get();
		exit(1);
	}
	return animDecoder;
}

BOOL WINAPI ConsoleHandlerRoutine(DWORD dwCtrlType) {
	if (dwCtrlType == CTRL_CLOSE_EVENT) {
		printf("[*] shutting down");
		WSACleanup();
		return TRUE;
	}
	return FALSE;
}

DWORD WINAPI AnimServMain(LPVOID lpParameter) {
	spawnConsole();
	//waitForDbg();
	AnimDecoder animDecoder = findAnimDecoder();
	SetConsoleCtrlHandler(ConsoleHandlerRoutine, TRUE);
	if (startServerSocket(animDecoder) != 0) {
		std::cin.get();
		exit(1);
	}
	return 0;
}

extern "C" {
	BOOL WINAPI DllMain(HANDLE hDllHandle, DWORD dwReason, LPVOID lpreserved) {
		switch (dwReason) {
		case DLL_PROCESS_ATTACH:
			const HANDLE thread = CreateThread(NULL, 0, AnimServMain, NULL, 0, NULL);
			if (thread) {
				CloseHandle(thread);
			}
			break;
		}
		return TRUE;
	}
};
