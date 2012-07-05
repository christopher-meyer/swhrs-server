package no.steria.swhrs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mongodb.DB;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;

public class PersonServlet extends HttpServlet {

	private static final long serialVersionUID = -1090477374982937503L;
	private DB db;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Set<String> colls = db.getCollectionNames();
		PrintWriter writer = resp.getWriter();
		
		writer.append("<html><body>HHer er servlet<ul>");
		for (String collectionName : colls) {
			writer.append("<li>" + collectionName + "</li>");
		}
		writer.append("</ul></body></html>");
	}
	
	@Override
	public void init() throws ServletException {
//		URI mongoURI = new URI(System.getenv("MONGOHQ_URL"));
		System.out.println("Connecting to mongo");
		MongoURI mongoURI = new MongoURI(System.getenv("MONGOHQ_URL"));
		try {
			db = mongoURI.connectDB();
			db.authenticate(mongoURI.getUsername(), mongoURI.getPassword());
		} catch (MongoException e) {
			throw new ServletException(e);
		} catch (UnknownHostException e) {
			throw new ServletException(e);
		}
		System.out.println("Connected");
	}
}
