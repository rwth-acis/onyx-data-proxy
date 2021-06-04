package i5.las2peer.services.onyxDataProxyService.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

public class ZipHelper {
	public static void extractFiles(InputStream fileInputStream, String path) {
		ZipInputStream zis = new ZipInputStream(fileInputStream);
		File destDir = new File(path);
		try {
			if (!destDir.exists()) {
				FileUtils.forceMkdir(destDir.getAbsoluteFile());
			} else {
				FileUtils.cleanDirectory(destDir.getAbsoluteFile());
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		byte[] buffer = new byte[1024];
		try {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				if (!zipEntry.getName().contains("/")) {
					File newFile = ZipHelper.newFile(destDir, zipEntry);
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}
}
