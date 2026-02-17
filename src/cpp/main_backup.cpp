#include <CascLib.h>
#include <SimpleOpt.h>
#include <iostream>
#include <string>
#include <vector>
#include <unordered_set>
#include <algorithm>
#include <fstream>

#ifdef _WIN32
    #include <windows.h>
#else
    #include <unistd.h>
    #include <dirent.h>
    #include <sys/stat.h>
#endif

using namespace std;

struct tSearchResult {
    string strFileName;
    string strFullPath;
};

enum
{
    OPT_HELP,
    OPT_LISTFILE,
    OPT_SEARCH,
    OPT_EXTRACT,
    OPT_DEST,
    OPT_FULLPATH,
    OPT_LOWERCASE,
    OPT_ALL,
};

const CSimpleOpt::SOption COMMAND_LINE_OPTIONS[] = {
    { OPT_HELP,             "-h",               SO_NONE    },
    { OPT_HELP,             "--help",           SO_NONE    },
    { OPT_LISTFILE,         "-l",               SO_REQ_SEP },
    { OPT_LISTFILE,         "--listfile",       SO_REQ_SEP },
    { OPT_SEARCH,           "-s",               SO_REQ_SEP },
    { OPT_SEARCH,           "--search",         SO_REQ_SEP },
    { OPT_EXTRACT,          "-e",               SO_REQ_SEP },
    { OPT_EXTRACT,          "--extract",        SO_REQ_SEP },
    { OPT_DEST,             "-o",               SO_REQ_SEP },
    { OPT_DEST,             "--dest",           SO_REQ_SEP },
    { OPT_FULLPATH,         "-f",               SO_NONE    },
    { OPT_FULLPATH,         "--fullpath",       SO_NONE    },
    { OPT_LOWERCASE,        "-c",               SO_NONE    },
    { OPT_LOWERCASE,        "--lowercase",      SO_NONE    },
    { OPT_ALL,              "--all",            SO_NONE    },
    
    SO_END_OF_OPTIONS
};

void showUsage(const std::string& strApplicationName)
{
    cout << "D2RCascCLI" << endl
         << "Usage: " << strApplicationName << " [options] <CASC_ROOT> <PATTERN>" << endl
         << endl
         << "This program can extract files from a CASC storage" << endl
         << endl
         << "Options:" << endl
         << "    --help, -h:              Display this help" << endl
         << "    --listfile <FILE>" << endl
         << "    -l <FILE>                Use the list file FILE [required if PATTERN is not a full path]" << endl
         << "    --dest <PATH>," << endl
         << "    -o <PATH>:               The folder where the files are extracted (default: the" << endl
         << "                             current one)" << endl
         << "    --fullpath, -f:          During extraction, preserve the path hierarchy found" << endl
         << "                             inside the storage" << endl
         << "    --lowercase, -c:         Convert extracted file paths to lowercase" <<endl
         << "    --all:                   Output all available CASC hiearchy into the specified listfile" <<endl
         << endl
         << "Examples:" << endl
         << endl
         << "  1) Extract all the *.M2 files in a CASC storage:" << endl
         << endl
         << "       ./D2RCascCLI -l listfile-wow6.txt \"/Applications/World of Warcraft Beta/Data/\" *.M2" << endl
         << endl
         << "  2) Extract a specific file from a CASC storage:" << endl
         << "       IMPORTANT: The file name must be enclosed in \"\" to prevent the shell to" << endl
         << "                  interpret the \\ character as the start of an escape sequence." << endl
         << endl
         << "       ./D2RCascCLI -o out \"/Applications/World of Warcraft Beta/Data/\" \"Path\\To\\The\\File\"" << endl
         << endl
         << "  3) Extract some specific files from a CASC storage, preserving the path hierarchy:" << endl
         << endl
         << "       ./D2RCascCLI -f -o out -l listfile-wow6.txt \"/Applications/World of Warcraft Beta/Data/\" \"Path\\To\\Extract\\*\"" << endl
         << endl;
}

int ListFilesInCASC(HANDLE hStorage, const std::string& searchPattern, const std::string& listFile)
{
	DWORD err;
	if (DeleteFileA(listFile.c_str()) == 0) {
        err = GetLastError();
        if (err != ERROR_FILE_NOT_FOUND) { // ignore if it didn't exist
            std::cerr << "Failed to delete existing list file '" << listFile << "' Error: " << err << std::endl;
            return -1;
        }
		else {
			SetLastError(NULL);
		}
    }
	
    CASC_FIND_DATA findData{};
    std::vector<std::string> results;
    std::unordered_set<std::string> seen;
    HANDLE handle = CascFindFirstFile(hStorage, searchPattern.c_str(), &findData, NULL);
	
    do {
		err = GetLastError();
		if (err) {
			std::cerr << "Casc find failed with error: " << err << std::endl;
			return err;
		}
	
        std::string name = findData.szFileName;

        // Remove leading "data:" if present
        //if (name.rfind("data:", 0) == 0) {
        //    name = name.substr(5);
        //}

        // normalize slashes
        for (auto& c : name) if (c == '\\') c = '/';

        if (name.find("data/global/excel/") != std::string::npos &&
            name.size() > 4 &&
            name.substr(name.size() - 4) == ".txt") 
        {
            if (seen.insert(name).second) {
                std::cout << name << std::endl;
                results.push_back(name);
            }
        }

    } while (CascFindNextFile(handle, &findData));
	
    if (!CascFindClose(handle)) {
		err = GetLastError();
		std::cerr << "CascFindClose failed with error: " << err << std::endl;
		return err;
	}

    std::ofstream outFile(listFile);
    if (!outFile.is_open()) {
        std::cerr << "Failed to open list file for writing: " << listFile << std::endl;
        return -1;
    }
	
	err = GetLastError();
    if (err) {
        std::cerr << "outFile opening failed with error: " << err << std::endl;
        return err;
    }

    for (const auto& f : results) {
        outFile << f << std::endl;
        err = GetLastError();
		if (err) {
			cerr << "outFile append failed on '" << f << "' with error: " << err << endl;
			return err;
		}
    }

	CascCloseStorage(hStorage);
	err = GetLastError();
	if (err) {
		cerr << "Casc close storage failed with error: " << err << endl;
		return err;
	}
    std::cout << "Finished writing file list to " << listFile << std::endl;
    return 0;
}


int main(int argc, char** argv)
{
	try {
		HANDLE hStorage;
		string strListFile;
		string strSearchPattern;
		string strStorage;
		string strDestination = ".";
		vector<tSearchResult> searchResults;
		bool bUseFullPath = false;
		bool bLowerCase = false;
		bool bAllMode = false;
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

					case OPT_LISTFILE:
						strListFile = args.OptionArg();
						cout << "Listfile specified: " << strListFile << endl;
						break;

					case OPT_DEST:
						strDestination = args.OptionArg();
						cout << "Destination directory: " << strDestination << endl;
						break;

					case OPT_FULLPATH:
						bUseFullPath = true;
						cout << "Full path preservation enabled." << endl;
						break;

					case OPT_LOWERCASE:
						bLowerCase = true;
						cout << "Lowercase conversion enabled." << endl;
						break;
						
					case OPT_ALL:
						bAllMode = true;
						break;
				}
			}
			else
			{
				cerr << "Invalid argument: " << args.OptionText() << endl;
				return -1;
			}
		}
		
		if (bAllMode)
        {
            if (args.FileCount() < 1)
            {
                cerr << "Error: Must specify the path to a CASC storage" << endl;
                return -1;
            }

            strStorage = args.File(0);

            // Default search pattern for --all
            if (args.FileCount() >= 2)
                strSearchPattern = args.File(1);
            else
                strSearchPattern = "*"; // Use wildcard if no search pattern is provided

            cout << "Opening CASC storage at '" << strStorage << "'..." << endl;

            if (!CascOpenStorage(strStorage.c_str(), 0, &hStorage))
            {
                cerr << "Failed to open storage. Error: " << GetLastError() << endl;
                return -1;
            }

            if (strListFile.empty())
            {
                cerr << "Error: --all requires --listfile" << endl;
                return -1;
            }

            // List files in CASC and write them to the list file
            ListFilesInCASC(hStorage, strSearchPattern, strListFile);
			
			return 0;
        }

        if (args.FileCount() != 2)
        {
            cerr << "Error: Must specify both the path to a CASC storage and a search pattern" << endl;
            return -1;
        }

        strStorage = args.File(0);
        strSearchPattern = args.File(1);

        // Remove trailing slashes at the end of the storage path (CascLib doesn't like that)
        if ((strStorage[strStorage.size() - 1] == '/') || (strStorage[strStorage.size() - 1] == '\\'))
            strStorage = strStorage.substr(0, strStorage.size() - 1);

        cout << "Opening CASC storage at '" << strStorage << "'..." << endl;

        // Check if the storage path exists
        if (GetFileAttributes(strStorage.c_str()) == INVALID_FILE_ATTRIBUTES)
        {
            cerr << "Error: The CASC storage path does not exist or is incorrect." << endl;
            return -1;
        }

        // Try to open CASC storage and log the error if failed
        if (!CascOpenStorage(strStorage.c_str(), 0, &hStorage))
        {
            DWORD dwError = GetLastError();
            cerr << "Failed to open the storage '" << strStorage << "'. Error: " << dwError << endl;
            return -1;
        }
        else
        {
            cout << "Successfully opened CASC storage." << endl;
        }

        // Read listfile into memory
        std::unordered_set<std::string> listFiles;
        if (!strListFile.empty())
        {
            ifstream listFileStream(strListFile);
            if (!listFileStream.is_open()) {
                cerr << "Failed to open listfile: " << strListFile << endl;
                return -1;
            }

            string line;
			cout << "Casc search criteria:" << endl;
            while (getline(listFileStream, line)) {
                listFiles.insert(line);
					cout << "  - " << line << endl;
            }
            listFileStream.close();
        }
        else {
            cerr << "Error: No listfile specified. Use the --listfile option." << endl;
            return -1;
        }

        // Search the files in CASC with wildcard pattern "*"
        cout << "Searching for files with pattern: '" << strSearchPattern << "'..." << endl;

        CASC_FIND_DATA findData;
		HANDLE handle = CascFindFirstFile(hStorage, strSearchPattern.c_str(), &findData, NULL);

		if (handle)
		{
			cout << "Found files:" << endl;

			do {
				string foundFile = findData.szFileName;
				
				// Normalize slashes
				for (auto& c : foundFile) if (c == '\\') c = '/';

				// Check if this file is in the list
				if (listFiles.find(foundFile) != listFiles.end()) {
					cout << "  - " << foundFile << endl;

					tSearchResult r;
					r.strFileName = findData.szPlainName;
					r.strFullPath = foundFile;
					searchResults.push_back(r);
				}

			} while (CascFindNextFile(handle, &findData)); // Continue to the next file

			CascFindClose(handle);  // Close the handle after the loop is finished
		}
		else
		{
			cerr << "No files found matching pattern: '" << strSearchPattern << "'." << endl;
		}

        // Extraction
        if (!searchResults.empty())
        {
            cout << endl;
            cout << "Starting extraction..." << endl;

            if (strDestination.at(strDestination.size() - 1) != '/')
                strDestination += "/";

            vector<tSearchResult>::iterator iter, iterEnd;
            for (iter = searchResults.begin(), iterEnd = searchResults.end(); iter != iterEnd; ++iter)
            {
                string strDestName = strDestination;

                cout << "Processing file: " << iter->strFullPath << endl;

                if (bUseFullPath)
                {
                    if (bLowerCase)
                    {
                        transform(iter->strFullPath.begin(), iter->strFullPath.end(), iter->strFullPath.begin(), ::tolower);
                        cout << "Converted path to lowercase: " << iter->strFullPath << endl;
                    }

                    strDestName += iter->strFullPath;

                    size_t offset = strDestName.find("\\");
                    while (offset != string::npos)
                    {
                        strDestName = strDestName.substr(0, offset) + "/" + strDestName.substr(offset + 1);
                        offset = strDestName.find("\\");
                    }

                    offset = strDestName.find_last_of("/");
                    if (offset != string::npos)
                    {
                        string dest = strDestName.substr(0, offset + 1);

                        size_t start = dest.find("/", 0);
                        while (start != string::npos)
                        {
                            string dirname = dest.substr(0, start);
                            cout << "Creating directory: " << dirname << endl;

                            // Platform-specific mkdir
                            #ifdef _WIN32
                                CreateDirectory(dirname.c_str(), NULL);  // Windows
                            #else
                                DIR* d = opendir(dirname.c_str());
                                if (!d)
                                {
                                    mkdir(dirname.c_str(), S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH); // Unix
                                    cout << "Created directory: " << dirname << endl;
                                }
                                else
                                {
                                    closedir(d);
                                }
                            #endif

                            start = dest.find("/", start + 1);
                        }
                    }
                }
                else
                {
                    if (bLowerCase)
                    {
                        transform(iter->strFileName.begin(), iter->strFileName.end(), iter->strFileName.begin(), ::tolower);
                        cout << "Converted file name to lowercase: " << iter->strFileName << endl;
                    }

                    strDestName += iter->strFileName;
                }

                HANDLE hFile;
                if (CascOpenFile(hStorage, iter->strFullPath.c_str(), CASC_LOCALE_ALL, 0, &hFile))
                {
                    DWORD read;
                    FILE* dest = fopen(strDestName.c_str(), "wb");
                    if (dest)
                    {
                        cout << "Extracting file to: " << strDestName << endl;
                        do {
                            if (CascReadFile(hFile, &buffer, 1000000, &read))
                                fwrite(&buffer, read, 1, dest);
                        } while (read > 0);

                        fclose(dest);
                    }
                    else
                    {
                        cerr << "Failed to extract the file '" << iter->strFullPath << "' to " << strDestName << endl;
                    }

                    CascCloseFile(hFile);
                }
                else
                {
                    cerr << "Failed to open file for extraction: " << iter->strFullPath << endl;
                }
            }
        }
        else
        {
            cerr << "No files to extract." << endl;
        }

        cout << "Extraction completed." << endl;
        CascCloseStorage(hStorage);
    }
    catch (...) {
        std::cerr << "Caught an unknown exception!" << std::endl;
    }

    return 0;
}
