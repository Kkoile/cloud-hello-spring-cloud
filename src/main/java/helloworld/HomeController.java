package helloworld;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController
{
	@Autowired(required = false)
	DataSource dataSource;
	
	@Autowired(required = false)
	RedisConnectionFactory redisConnectionFactory;
	
	@Autowired(required = false)
	MongoDbFactory mongoDbFactory;
	
	@Autowired(required = false)
	ConnectionFactory rabbitConnectionFactory;

	@Autowired
	ApplicationInstanceInfo instanceInfo;

	@RequestMapping("/")
	public String home(Model model)
	{
		Map<Class<?>, String> services = new LinkedHashMap<Class<?>, String>();
		
		if (dataSource != null) services.put(getClass(dataSource), toString(dataSource));
		if (mongoDbFactory != null)  services.put(getClass(mongoDbFactory), toString(mongoDbFactory));
		if (redisConnectionFactory != null)  services.put(getClass(redisConnectionFactory), toString(redisConnectionFactory));
		if (rabbitConnectionFactory != null) services.put(getClass(rabbitConnectionFactory), toString(rabbitConnectionFactory));
		
		model.addAttribute("services", services.entrySet());
		model.addAttribute("instanceInfo", instanceInfo);
		model.addAttribute("env", System.getenv());

		return "home";
	}

	private String toString(DataSource dataSource)
	{
		if (dataSource == null)
		{
			return "<none>";
		}
		else
		{
			Connection conn = null;
			
			try
	        {
	        	conn = dataSource.getConnection();
	        	DatabaseMetaData metaData = conn.getMetaData();
	        	
	        	return stripCredentials(metaData.getURL());
	        }
	        catch (Exception ex)
	        {
	        	return "<unknown> " + dataSource.getClass();
	        }
	        finally
	        {
	        	if (conn != null)
	        	{
	        		try
	                {
		                conn.close();
	                }
	                catch (SQLException e) {} // we are screwed!
	        	}
	        }	
		}
	}

	private String toString(MongoDbFactory mongoDbFactory)
	{
		if (mongoDbFactory == null)
		{
			return "<none>";
		}
		else
		{
			try
			{
				return mongoDbFactory.getDb().getMongo().getAddress().toString();
			}
			catch (Exception ex)
			{
				return "<invalid address> " + mongoDbFactory.getDb().getMongo().toString();
			}
		}
	}

	private String toString(RedisConnectionFactory redisConnectionFactory)
	{
		if (redisConnectionFactory == null)
		{
			return "<none>";
		}
		else
		{
			if (redisConnectionFactory instanceof JedisConnectionFactory)
			{
				JedisConnectionFactory jcf = (JedisConnectionFactory) redisConnectionFactory;
				return jcf.getHostName().toString() + ":" + jcf.getPort();
			}
			else if (redisConnectionFactory instanceof LettuceConnectionFactory)
			{
				LettuceConnectionFactory lcf = (LettuceConnectionFactory) redisConnectionFactory;
				return lcf.getHostName().toString() + ":" + lcf.getPort();
			}
			return "<unknown> " + redisConnectionFactory.getClass();
		}
	}

	private String toString(ConnectionFactory rabbitConnectionFactory)
	{
		if (rabbitConnectionFactory == null)
		{
			return "<none>";
		}
		else
		{
			return rabbitConnectionFactory.getHost() + ":" + rabbitConnectionFactory.getPort();
		}
	}

	private String stripCredentials(String urlString)
	{
		try
		{
			if (urlString.startsWith("jdbc:"))
			{
				urlString = urlString.substring("jdbc:".length());
			}
			URI url = new URI(urlString);
			return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), null, null).toString();
		}
		catch (URISyntaxException e)
		{
			System.out.println(e);
			return "<bad url> " + urlString;
		}
	}
	
	private static Class<?> getClass(Object obj)
	{
		if (obj != null)
		{
			return obj.getClass();
		}
		else
		{
			return null;
		}
	}

}