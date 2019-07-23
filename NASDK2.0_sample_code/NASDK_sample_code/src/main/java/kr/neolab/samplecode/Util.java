package kr.neolab.samplecode;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;

import kr.neolab.samplecode.provider.DbOpenHelper;

public class Util
{
	public static void showToast( Context context, final String msg )
	{
		Toast.makeText( context, msg, Toast.LENGTH_SHORT ).show();
	}

	public static int[] convertIntegers( List<Integer> integers)
	{
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = iterator.next().intValue();
		}
		return ret;
	}

	public static void spliteExport(Context context)
	{
		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();

			if (sd.canWrite()) {
				String currentDBPath = "/data/kr.neolab.samplecode/databases/" + DbOpenHelper.DATABASE_NAME;
				String backupDBPath = Const.SAMPLE_FOLDER_PATH +"/"+ "samplecode.db";
				File currentDB = new File(data, currentDBPath);
				File backupDB = new File( backupDBPath);

				if (currentDB.exists()) {
					FileChannel src = new FileInputStream( currentDB).getChannel();
					FileChannel dst = new FileOutputStream( backupDB).getChannel();
					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
				}
				if(backupDB.exists()){
					Toast.makeText(context, "DB Export Complete!!", Toast.LENGTH_SHORT).show();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
