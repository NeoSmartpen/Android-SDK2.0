package kr.neolab.samplecode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;

import kr.neolab.samplecode.provider.DbOpenHelper;
import kr.neolab.samplecode.renderer.Renderer2;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;

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

	public static Bitmap StrokeToImage(Stroke[] strokes, float scale)
	{
		float minX, minY, maxX, maxY, offsetX, offsetY ;
		int width, height;

		if( strokes == null || strokes.length == 0 )
			return null;

		Dot fd = strokes[0].getDots().get( 0 );
		minX = fd.getX();
		minY = fd.getY();
		maxX = (int)fd.getX();
		maxY = (int)fd.getY();

		// get strokes offset, width, height
		for( Stroke stroke : strokes )
		{
			for( Dot d : stroke.getDots() )
			{
				// get min X
				if( minX > d.getX() )
					minX = d.getX();

				// get min Y
				if( minY > d.getY() )
					minY = d.getY();


				// get max X
				if( maxX < d.getX() )
					maxX = (int)d.getX();

				// get max Y
				if( maxY < d.getY() )
					maxY = (int)d.getY();
			}
		}

		minX *= scale;
		minY *= scale;
		maxX *= scale;
		maxY *= scale;

		// 반올림
		width = (int)((maxX - minX) + 0.5f);
		height = (int)((maxY - minY) + 0.5f);

		offsetX = minX;
		offsetY = minY;

		// 여백 만들기
		width = width + 25;
		height = height + 25;
		offsetX = offsetX - 7;
		offsetY = offsetY - 7;

		Bitmap bitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
		bitmap.eraseColor( Color.WHITE );
		Canvas canvas = new Canvas(bitmap);

		Renderer2.draw( canvas,  strokes, scale, -offsetX, -offsetY );

		return bitmap;
	}
}
