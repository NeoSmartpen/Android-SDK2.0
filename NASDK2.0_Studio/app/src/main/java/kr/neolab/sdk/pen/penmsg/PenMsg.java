package kr.neolab.sdk.pen.penmsg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The structure for sent message from pen
 *
 * @author CHY
 */
public class PenMsg
{
	/**
	 * The Pen msg type.
	 */
	public int penMsgType;

	/**
	 * The Content.
	 */
	public String content;

	/**
	 * The Mac address.
	 */
	public String sppAddress = "";

	/**
	 * Instantiates a new Pen msg.
	 *
	 * @param penMsgType the pen msg type
	 */
	public PenMsg( int penMsgType )
	{
		this.penMsgType = penMsgType;
	}

	/**
	 * Instantiates a new Pen msg.
	 *
	 * @param penMsgType the pen msg type
	 * @param MACAddress the mac address
	 */
	public PenMsg( int penMsgType, String MACAddress )
	{
		this.sppAddress = MACAddress;
		this.penMsgType = penMsgType;
	}

//	public PenMsg( int penMsgType, String content )
//	{
//		this.penMsgType = penMsgType;
//		this.content = content;
//	}

	/**
	 * Instantiates a new Pen msg.
	 *
	 * @param penMsgType the pen msg type
	 * @param job        the job
	 */
	public PenMsg( int penMsgType, JSONObject job )
	{
		this.penMsgType = penMsgType;
		
		try
		{
			this.content = toJSONString(job);
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Instantiates a new Pen msg.
	 *
	 * @param penMsgType the pen msg type
	 * @param jarr       the jarr
	 */
	public PenMsg( int penMsgType, JSONArray jarr )
	{
		this.penMsgType = penMsgType;
		
		try
		{
			this.content = toJSONString(jarr);
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Instantiates a new Pen msg.
	 *
	 * @param penMsgType the pen msg type
	 * @param name       the name
	 * @param value      the value
	 */
	public PenMsg( int penMsgType, String name, String value )
	{
		this.penMsgType = penMsgType;
		
		try
		{
			this.content = toJSONString( name, value );
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Instantiates a new Pen msg.
	 *
	 * @param penMsgType the pen msg type
	 * @param names      the names
	 * @param values     the values
	 */
	public PenMsg( int penMsgType, String[] names, String[] values )
	{
		this.penMsgType = penMsgType;
		
		try
		{
			this.content = toJSONString(names, values);
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Gets pen msg type.
	 *
	 * @return the pen msg type
	 */
	public int getPenMsgType()
	{
		return penMsgType;
	}

	/**
	 * Gets content.
	 *
	 * @return the content
	 */
	public String getContent()
	{
		return content;
	}

	/**
	 * Gets content by json object.
	 *
	 * @return the content by json object
	 */
	public JSONObject getContentByJSONObject()
	{
		return getContentByJSONObject(content);
	}

	/**
	 * Gets content by json array.
	 *
	 * @return the content by json array
	 */
	public JSONArray getContentByJSONArray()
	{
		return getContentByJSONArray(content);
	}

	/**
	 * Gets content by json object.
	 *
	 * @param content the content
	 * @return the content by json object
	 */
	public static JSONObject getContentByJSONObject(String content)
	{
		JSONObject result = null;
		
		try
		{
			result = new JSONObject(content);
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * Gets content by json array.
	 *
	 * @param content the content
	 * @return the content by json array
	 */
	public static JSONArray getContentByJSONArray(String content)
	{
		JSONArray result = null;
		
		try
		{
			result = new JSONArray(content);
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * To json string string.
	 *
	 * @param name  the name
	 * @param value the value
	 * @return the string
	 * @throws JSONException the json exception
	 */
	public String toJSONString(String name, String value) throws JSONException
	{
		JSONObject job = new JSONObject();
		
		job.put( name, value );
		
		return job.toString();
	}

	/**
	 * To json string string.
	 *
	 * @param names  the names
	 * @param values the values
	 * @return the string
	 * @throws JSONException the json exception
	 */
	public String toJSONString(String[] names, String[] values) throws JSONException
	{
		if ( names.length != values.length )
		{
			return null;
		}
		
		JSONObject job = new JSONObject();
		
		for (int i=0; i<names.length; i++)
		{
			job.put( names[i], values[i] );
		}
		
		return job.toString();
	}

	/**
	 * To json string string.
	 *
	 * @param map the map
	 * @return the string
	 * @throws JSONException the json exception
	 */
	public String toJSONString( LinkedHashMap<String, String> map ) throws JSONException
	{
		JSONObject job = new JSONObject();
		
		Set<Entry<String, String>> entrys = map.entrySet();
		
		for (Entry<String, String> item : entrys)
		{
			job.put( item.getKey(), item.getValue() );
		}
		
		return job.toString();
	}

	/**
	 * To json string string.
	 *
	 * @param jarr the jarr
	 * @return the string
	 * @throws JSONException the json exception
	 */
	public String toJSONString( JSONArray jarr ) throws JSONException
	{
		return jarr.toString();
	}

	/**
	 * To json string string.
	 *
	 * @param job the job
	 * @return the string
	 * @throws JSONException the json exception
	 */
	public String toJSONString( JSONObject job ) throws JSONException
	{
		return job.toString();
	}
}
