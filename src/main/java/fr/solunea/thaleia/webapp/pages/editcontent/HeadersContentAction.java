package fr.solunea.thaleia.webapp.pages.editcontent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import fr.solunea.thaleia.utils.DetailedException;

public class HeadersContentAction extends AbstractEditContentAction {

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response)
			throws DetailedException {
		
		
		String urlToCheck = request.getParameter("urlToCheck");
		if (urlToCheck == null) {
			urlToCheck = "";
		}
		
		URL obj;
		URLConnection conn = null;
		try {
			obj = new URL(urlToCheck);
			conn = obj.openConnection();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//get all headers
		String responseToSend = new Gson().toJson(conn.getHeaderFields());
		
		ok(response, responseToSend);
	}
}
