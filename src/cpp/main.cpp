#include <CascLib.h>
#include <SimpleOpt.h>
#include <iostream>
#include <string>
#include <vector>
#include <unordered_set>
#include <algorithm>
#include <fstream>
#include <windows.h>

using namespace std;

enum
{
    OPT_HELP,
    OPT_SOURCE,
    OPT_DEST,
    OPT_PATTERN,
};

const CSimpleOpt::SOption COMMAND_LINE_OPTIONS[] = {
    { OPT_HELP,             "-h",               SO_NONE    },
    { OPT_HELP,             "--help",           SO_NONE    },
    { OPT_SOURCE,           "--source",         SO_REQ_SEP },
    { OPT_SOURCE,           "-s",               SO_REQ_SEP },
    { OPT_DEST,             "--dest",           SO_REQ_SEP },
    { OPT_DEST,             "-d",               SO_REQ_SEP },
    { OPT_PATTERN,          "--pattern",        SO_REQ_SEP },
    { OPT_PATTERN,          "-p",               SO_REQ_SEP },
    SO_END_OF_OPTIONS
};

void showUsage(const string strApplicationName)
{
    cout << "D2RCascCLI" << endl
         << "Usage: " << strApplicationName << " [options] <CASC_ROOT> <PATTERN>" << endl
         << endl
         << "This program can extract files from a CASC storage" << endl
         << endl
         << "Options:" << endl
         << "    --help, -h:              Display this help" << endl
         << "    --source <PATH>," << endl
		 << "    -s <PATH>:               Source directory of D2R for latest extraction" <<endl
         << "    --dest <PATH>," << endl
         << "    -d <PATH>:               The folder where the files are extracted (default: current dir)" << endl
         << "    --pattern <pattern>," << endl
		 << "    -p <pattern>:            Casc patttern search (default: *)" <<endl
         << endl
         << "Examples:" << endl
		 << "    D2RCascCLI -s \"D:/Diablo II Resurrected/\""
         << endl;
}

bool CreateDirectoryTree(const string& path) {
    DWORD err;
    size_t pos = path.find_last_of('/');
    string buildPath = path.substr(0, pos);
    if (path == ".") return true;
	
    DWORD dwAttrib = GetFileAttributes(path.c_str());
    if (dwAttrib == INVALID_FILE_ATTRIBUTES || !(dwAttrib & FILE_ATTRIBUTE_DIRECTORY)) {
        if (!CreateDirectoryTree(buildPath)) {
            return false;
        }

        if (!CreateDirectory(path.c_str(), NULL)) {
            err = GetLastError();
			
            if (err == ERROR_ALREADY_EXISTS) {
                return true;
            }

            cerr << "Failed to create directory: " << path << ", Code: " << err << endl;
            return false;
        }
        else {
            cout << "Directory created: " << path << endl;
        }
    }

    return true;
}

bool DeleteDirectory(const string& dirPath) {
    // Check if the directory exists
    DWORD dwAttrib = GetFileAttributes(dirPath.c_str());
    if (dwAttrib == INVALID_FILE_ATTRIBUTES || !(dwAttrib & FILE_ATTRIBUTE_DIRECTORY)) {
        cerr << "Directory does not exist: " << dirPath << endl;
        return false;
    }

    // Get a handle to the directory
    HANDLE hFind;
    WIN32_FIND_DATA findData;

    // Ensure the directory path ends with a backslash
    string searchPath = dirPath;
    if (searchPath.back() != '\\') {
        searchPath += '\\';
    }
    searchPath += "*";  // This will match all files and subdirectories

    // Open the directory to list its contents
    hFind = FindFirstFile(searchPath.c_str(), &findData);
    if (hFind == INVALID_HANDLE_VALUE) {
        cerr << "Failed to open directory: " << dirPath << endl;
        return false;
    }

    do {
        // Skip the "." and ".." directories
        if (string(findData.cFileName) == "." || string(findData.cFileName) == "..") {
            continue;
        }

        string fullPath = dirPath + "\\" + findData.cFileName;

        // If it's a directory, recurse into it
        if (findData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
            if (!DeleteDirectory(fullPath)) {
                return false; // Return false if we fail to delete a subdirectory
            }
        } else {
            // It's a file, delete it
            if (DeleteFileA(fullPath.c_str()) == 0) {
                DWORD err = GetLastError();
                cerr << "Failed to delete file '" << fullPath << "' Error: " << err << endl;
                return false;
            }
        }
    } while (FindNextFile(hFind, &findData) != 0);

    // Close the handle after traversing the directory
    FindClose(hFind);

    // Now delete the empty directory itself
    if (RemoveDirectory(dirPath.c_str()) == 0) {
        DWORD err = GetLastError();
        cerr << "Failed to remove directory '" << dirPath << "' Error: " << err << endl;
        return false;
    }

    return true;
}

int main(int argc, char** argv)
{
	HANDLE hStorage;
	DWORD err;
	string strListFile = "list-file-d2r.txt";
	string strSourceDir = "";
	string strDestDir = ".";
	string strPattern = "*";
	char buffer[1000000];

	// Parse the command-line parameters
	cout << "Parsing command-line arguments..." << endl;
	CSimpleOpt args(argc, argv, COMMAND_LINE_OPTIONS);
	while (args.Next())
	{
		if (args.LastError() == SO_SUCCESS)
		{
			switch (args.OptionId())
			{
				case OPT_HELP:
					showUsage(argv[0]);
					return 0;

				case OPT_SOURCE:
					strSourceDir = args.OptionArg();
					cout << "Source directory: " << strSourceDir << endl;
					break;

				case OPT_DEST:
					strDestDir = args.OptionArg();
					cout << "Destination directory: " << strDestDir << endl;
					break;

				case OPT_PATTERN:
					strPattern = args.OptionArg();
					cout << "Casc Pattern: " << strPattern << endl;
					break;
			}
		}
		else
		{
			cerr << "Invalid argument: " << args.OptionText() << endl;
			return -1;
		}
	}
	
	if (strSourceDir.empty())
	{
		cerr << "Parameter not found: --source" << endl;
		return -1;
	}
	
	strListFile = strDestDir + "/" + strListFile;
	
	for (auto& c : strListFile) if (c == '\\') c = '/';
	for (auto& c : strDestDir) if (c == '\\') c = '/';

	cout << "Opening CASC storage at '" << strSourceDir << "'..." << endl;
	if (!CascOpenStorage(strSourceDir.c_str(), 0, &hStorage))
	{
		cerr << "Failed to open storage. Error: " << GetLastError() << endl;
		return -1;
	}
	
	cout << "Deleting old extraction file reference: " << strListFile << endl;
	if (DeleteFileA(strListFile.c_str()) == 0) {
		err = GetLastError();
		if (err != ERROR_FILE_NOT_FOUND) { // ignore if it didn't exist
			cerr << "Failed to delete existing list file '" << strListFile << "' Error: " << err << endl;
			return -1;
		}
		else {
			SetLastError(NULL);
		}
	}
	
	cout << "Deleting old extraction folder: " << strDestDir << endl;
	DeleteDirectory(strDestDir);

	cout << "Generating new extraction file reference: " << strListFile << endl;
	CASC_FIND_DATA findData{};
    vector<string> results;
    unordered_set<string> seen;
    HANDLE cascHandle = CascFindFirstFile(hStorage, strPattern.c_str(), &findData, NULL);
	
	do {
		if (!cascHandle) {
			err = GetLastError();
			cerr << "Casc find failed with error: " << err << endl;
			return err;
		}

		string strFullPath = findData.szFileName;
		for (auto& c : strFullPath) if (c == '\\') c = '/';
		
		if (strFullPath.size() > 4 &&
			strFullPath.substr(strFullPath.size() - 4) == ".txt" &&
			strFullPath.find("data/global") != string::npos &&
			strFullPath.find("data/global/excel/base/") == string::npos)
		{
			if (seen.insert(strFullPath).second) {
				cout << "Found: " << strFullPath << endl;
				results.push_back(strFullPath);
				
				size_t pos = strFullPath.find_last_of('/');
				string ref = strFullPath.substr(0, pos);
				if (ref.rfind("data:", 0) == 0) {
					ref = ref.substr(5);
				}
				
				string extractPath = strDestDir + "/" + ref;
				string filename = strFullPath.substr(pos + 1);
				cout << "Extracting File: " << filename << ", Extracting Location: " << extractPath << endl;
				CreateDirectoryTree(extractPath);
				
				HANDLE cascFile;
				string fileWb = extractPath + "/" + filename;
				if (CascOpenFile(hStorage, strFullPath.c_str(), CASC_LOCALE_ALL, 0, &cascFile))
				{
					DWORD read;
					FILE* dest = fopen(fileWb.c_str(), "wb");
					if (dest)
					{
						std::cout << "Extracting file to: " << fileWb << endl;
						do {
							if (CascReadFile(cascFile, &buffer, sizeof(buffer), &read))
								fwrite(&buffer, read, 1, dest);
						} while (read > 0);

						fclose(dest);
					}
					else
					{
						cerr << "Failed to extract the file '" << strFullPath << "' to " << fileWb << endl;
					}

					CascCloseFile(cascFile);
				}
				else
				{
					cerr << "Failed to open file for extraction: " << strFullPath << endl;
				}
			}
		}
		
		
	} while(CascFindNextFile(cascHandle, &findData));

	if (!CascFindClose(cascHandle)) {
		err = GetLastError();
		cerr << "CascFindClose failed with error: " << err << endl;
		return err;
	}
	else {
		cout << "Casc find closed" << endl;
	}
	
	ofstream outFile(strListFile);
	if (!outFile.is_open()) {
		cerr << "Failed to open extraction file reference for writing: " << strListFile << endl;
		return -1;
	}

	for (const auto& f : results) {
		outFile << f << endl;
	}

	cout << "Finished writing extraction file reference: " << strListFile << endl;
	
	if (!CascCloseStorage(hStorage)) {
		err = GetLastError();
		cerr << "Casc close storage failed with error: " << err << endl;
		return err;
	}
	else {
		cout << "Casc storage closed" << endl;
	}
	
	return 0;
}
