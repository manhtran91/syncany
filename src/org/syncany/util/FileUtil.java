/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUtil {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getSimpleName());    

    public static String getRelativePath(File base, File file) {
        //System.err.println("rel path = base = "+base.getAbsolutePath() + " - file: "+file.getAbsolutePath()+ " ---> ");
        if (base.getAbsolutePath().length() >= file.getAbsolutePath().length()) {
            return "";
        }

        String relativeFilePath = file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
        
        // Remove trailing slashes
        while (relativeFilePath.endsWith(File.separator)) {
        	relativeFilePath = relativeFilePath.substring(0, relativeFilePath.length()-1);
        }

        // Remove leading slashes
        while (relativeFilePath.startsWith(File.separator)) {
        	relativeFilePath = relativeFilePath.substring(1);
        }
        
        return relativeFilePath;
    }

    public static String getAbsoluteParentDirectory(File file) {
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
    }

    public static String getAbsoluteParentDirectory(String absFilePath) {
        return absFilePath.substring(0, absFilePath.lastIndexOf(File.separator));
    }

    public static String getRelativeParentDirectory(File base, File file) {
        //System.out.println(new File(getAbsoluteParentDirectory(file)));
        //System.err.println("reldir -> base = "+base.getAbsolutePath() + " - file: "+file.getAbsolutePath()+" ---> "+getRelativePath(base, new File(getAbsoluteParentDirectory(file))));
        return getRelativePath(base, new File(getAbsoluteParentDirectory(file)));
    }

    public static List<File> getRecursiveFileList(File root) throws FileNotFoundException {
        return getRecursiveFileList(root, false);
    }

    public static List<File> getRecursiveFileList(File root, boolean includeDirectories) throws FileNotFoundException {
        if (!root.isDirectory() || !root.canRead() || !root.exists()) {
            throw new FileNotFoundException("Invalid directory " + root);
        }

        List<File> result = getRecursiveFileListNoSort(root, includeDirectories);
        Collections.sort(result);

        return result;
    }

    private static List<File> getRecursiveFileListNoSort(File root, boolean includeDirectories) {
        List<File> result = new ArrayList<File>();
        List<File> filesDirs = Arrays.asList(root.listFiles());

        for (File file : filesDirs) {
            if (!file.isDirectory() || includeDirectories) {
                result.add(file);
            }

            if (file.isDirectory()) {
                List<File> deeperList = getRecursiveFileListNoSort(file, includeDirectories);
                result.addAll(deeperList);
            }
        }

        return result;
    }

    /**
     * Retrieves the extension of a file.
     * Example: "html" in the case of "/htdocs/index.html"
     *
     * @param file
     * @return
     */
    public static String getExtension(File file) {
        return getExtension(file.getName(), false);
    }

    public static String getExtension(File file, boolean includeDot) {
        return getExtension(file.getName(), includeDot);
    }

    public static String getExtension(String filename, boolean includeDot) {
        int dot = filename.lastIndexOf(".");

        if (dot == -1) {
            return "";
        }

        return ((includeDot) ? "." : "")
                + filename.substring(dot + 1, filename.length());
    }

    /**
     * Retrieves the basename of a file.
     * Example: "index" in the case of "/htdocs/index.html"
     * 
     * @param file
     * @return
     */
    public static String getBasename(File file) {
        return getBasename(file.getName());
    }

    public static String getBasename(String filename) {
        int dot = filename.lastIndexOf(".");

        if (dot == -1) {
            return filename;
        }

        return filename.substring(0, dot);
    }

    public static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return file;
        }
    }

    public static boolean renameVia(File fromFile, File toFile) {
        return renameVia(fromFile, toFile, ".ignore-rename-to-");
    }

    public static boolean renameVia(File fromFile, File toFile, String viaPrefix) {
        File tempFile = new File(toFile.getParentFile().getAbsoluteFile() + File.separator + viaPrefix + toFile.getName());
        FileUtil.deleteRecursively(tempFile); // just in case!	

        if (!fromFile.renameTo(tempFile)) {
            return false;
        }

        if (!tempFile.renameTo(toFile)) {
            tempFile.renameTo(fromFile);
            return false;
        }

        return true;
    }
    
    public static boolean deleteVia(File file) {
        return deleteVia(file, ".ignore-delete-from-");
    }
    
    public static boolean deleteVia(File file, String viaPrefix) {
        File tempFile = new File(file.getParentFile().getAbsoluteFile() + File.separator + viaPrefix + file.getName());
        FileUtil.deleteRecursively(tempFile); // just in case!	

        if (!file.renameTo(tempFile)) {
            return false;
        }

        if (!tempFile.delete()) {
            // If DELETE not successful; rename it back to the original filename
            tempFile.renameTo(file);
            return false;
        }
        
        return true;
    }        
    
    public static boolean mkdirVia(File folder) {
        return mkdirVia(folder, ".ignore-mkdir-");
    }
    
    public static boolean mkdirVia(File folder, String viaPrefix) {
        if (folder.exists()) {
            return true;
        }
        
        File canonFolder = null;
        
        try {
            canonFolder = folder.getCanonicalFile();
        } 
        catch (IOException e) {
            return false;
        }
        
        if (!canonFolder.getParentFile().exists()) {
            return false;
        }
        
        File tempFolder = new File(canonFolder.getParentFile()+File.separator+viaPrefix+canonFolder.getName());
        tempFolder.delete(); // Just in case
        
        if (!tempFolder.mkdir()) {
            return false;
        }
        
        if (!tempFolder.renameTo(canonFolder)) {
            tempFolder.delete();
            return false;
        }
        
        return true;
    }
    public static boolean mkdirsVia(File folder) {
        return mkdirsVia(folder, ".ignore-mkdirs-");
    }
    
    public static boolean mkdirsVia(File folder, String viaPrefix) {
        if (folder.exists()) {
            return true;
        }
 
        File canonFolder = null;
        
        try {
            canonFolder = folder.getCanonicalFile();
        } 
        catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Could not get canonical file for folder "+folder, e);
            }
            
            return false;
        }
        
        if (!canonFolder.getParentFile().exists()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "{0} does not exist. creating.", canonFolder.getParentFile());
            }
                
            if (!mkdirsVia(canonFolder.getParentFile(), viaPrefix)) {
                return false;
            }
        }
        
        return mkdirVia(canonFolder, viaPrefix);
    }    

    public static void copy(File src, File dst) throws IOException {
        copy(new FileInputStream(src), new FileOutputStream(dst));
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        // Performance tests say 4K is the fastest (sschellh)
        byte[] buf = new byte[4096];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }
    
    public static byte[] readFile(File file) throws IOException {
    	long fileLength = file.length();
    	
    	if (fileLength > 20*1024*1024) {
    		throw new IOException("File is larger than 20 MB. Should not load to memory.");
    	}
    	
        byte[] contents = new byte[(int) fileLength];

        FileInputStream fis = new FileInputStream(file);
        fis.read(contents);
        fis.close();

        return contents;
    }
    
    public static String readFileToString(File file) throws IOException {
    	StringBuffer fileData = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		char[] buf = new char[1024];
		int numRead=0;
		while((numRead=reader.read(buf)) != -1){
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}
		reader.close();
		
		return fileData.toString();
    }

    public static void writeToFile(byte[] bytes, File file) throws IOException {
        writeToFile(new ByteArrayInputStream(bytes), file);
    }

    public static void writeToFile(InputStream is, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);

        int read = 0;
        byte[] bytes = new byte[4096];

        while ((read = is.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }

        is.close();
        fos.close();
    }
    
    public static void appendToOutputStream(File fileToAppend, OutputStream outputStream) throws IOException {
    	appendToOutputStream(new FileInputStream(fileToAppend), outputStream);
    }
    
    public static void appendToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {    
        byte[] buf = new byte[4096];

        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);
        }

        inputStream.close();
    }

    public static boolean deleteRecursively(File file) {
        boolean success = true;

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                success = (f.isDirectory()) 
                    ? success && deleteRecursively(f)
                    : success && f.delete();
            }
        }

        success = success && file.delete();
        return success;
    }
    

	public static byte[] createChecksum(File file) throws Exception {
		return createChecksum(file, "SHA1");
	}
	
	public static byte[] createChecksum(File filename, String digestAlgorithm) throws Exception {
		FileInputStream fis =  new FileInputStream(filename);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance(digestAlgorithm);
		int numRead;

		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		fis.close();
		return complete.digest();
	}

}

