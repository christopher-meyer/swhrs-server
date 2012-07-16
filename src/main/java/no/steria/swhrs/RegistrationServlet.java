package no.steria.swhrs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.simple.JSONObject;


public class RegistrationServlet extends HttpServlet{
	
	private static final long serialVersionUID = -1090477374982937503L;
	private HourRegDao db;
		
	public void init() throws ServletException {
		db = new HibernateHourRegDao(Parameters.DB_JNDI);
	}
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		if (req.getRequestURL().toString().contains(("hours/list"))) { 
			resp.setContentType("application/json");
			//TODO currently hardcoded as LocalDate.now() change to get date parameter from javascript
			List<HourRegistration> hrlist = db.getAllHoursForDate(1, LocalDate.now());
			
			JSONObject json = createJsonObjectFromHours(hrlist);
			
			String jsonText = json.toString();
			System.out.println(jsonText);
			resp.getWriter().write(jsonText);
		}
		
	}

	@SuppressWarnings("unchecked")
	private JSONObject createJsonObjectFromHours(List<HourRegistration> hrlist) {
		JSONObject json = new JSONObject();
		for (HourRegistration hr: hrlist) {
			json.put(Integer.toString(hr.getProjectnumber()), hr.getHours());
		}
		return json;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		
		if (req.getRequestURL().toString().contains(("hours/registration"))) {
			int personId = Integer.parseInt(req.getParameter("personId"));
			String pNr = req.getParameter("projectNr");
			int projectNr = Integer.parseInt(req.getParameter("projectNr").trim());
			double hours = Double.parseDouble(req.getParameter("hours"));
			req.getParameter("date");
			
			System.out.println("Trying to save project: " + pNr);
			//TODO currently hardcoded as LocalDate.now() change to get date parameter from javascript
			saveRegToDatabase(personId, projectNr, LocalDate.now(), hours);
		}
		
		if (req.getRequestURL().toString().contains(("hours/login"))) {
			String username = req.getParameter("username");
			String password = req.getParameter("password");
			System.out.println("Username:" +username+" Password: "+password);
			int autoLoginExpire = (60*60*24);
			//Change this when database is up
			//if(db.validateUser(username, password) == true){
			if(username.equals("steria") && password.equals("123")){
				Cookie loginCookie = new Cookie("USERNAME", username);
				loginCookie.setMaxAge(autoLoginExpire);
				resp.setContentType("text/plain");
				PrintWriter writer = resp.getWriter();
				writer.append("Login ok");
			}else{
				resp.setStatus(403);
				System.out.println("FAIL");
			}
		}
		
		if(req.getRequestURL().toString().contains(("hours/week"))){
			String week = req.getParameter("week");		
			resp.setContentType("application/text");
			List<WeekRegistration> weeklist = db.getWeekSummary(week);
			JSONObject json = new JSONObject();
			int order = 0;
			
			for (WeekRegistration wr: weeklist) {
				order++;
				System.out.println("Date"+wr.getDate()+" Hours:"+wr.getWeekHours());
				json.put(wr.getDate(), order+":"+wr.getWeekHours());
			}
			resp.setContentType("text/json");
			PrintWriter writer = resp.getWriter();
			String jsonText = json.toString();
			writer.append(jsonText);
		}
		
 	}


	private void saveRegToDatabase(int personId, int projectNr, LocalDate date, double hours) {
		HourRegistration reg = HourRegistration.createRegistration(personId, projectNr, date, hours);
		db.saveHours(reg);
		System.out.println("Saving registration with data: " + projectNr + "," +hours+ ", " +LocalDate.now());
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		db.beginTransaction();
		super.service(req, resp);
		db.endTransaction(true);
		//TODO add a finally here so that it ends the transaction of the servlet crashes
	}

}
