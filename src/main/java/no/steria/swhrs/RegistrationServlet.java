package no.steria.swhrs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.LocalDate;


public class RegistrationServlet extends HttpServlet{
	
	private static final long serialVersionUID = -1090477374982937503L;
	private HibernateHourRegDao db;
		
	public void init() throws ServletException {
		db = new HibernateHourRegDao(Parameters.DB_JNDI);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();
		
		writer.append("<html><body>Dette er registrationservleten</body></html>");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		resp.setContentType("application/json");
		PrintWriter writer = resp.getWriter();		
		System.out.println("Dette er registrationservleten POST");
		writer.append("<html><body>Dette er registrationservletenPOST</body></html>");
		
		int personId = Integer.parseInt(req.getParameter("personId"));
		String favourite = req.getParameter("fav");
		String pNr = req.getParameter("projectNr");
		int projectNr = Integer.parseInt(req.getParameter("projectNr").trim());
		double hours = Double.parseDouble(req.getParameter("hours"));
		String lunch = req.getParameter("lunch");
		String date = req.getParameter("date");
		
		//String username = req.getParameter("username");
		//String password = req.getParameter("password");
		
		System.out.println(projectNr + "," +hours+ ", "+lunch+", "+date);
		
		saveRegToDatabase(personId, projectNr, LocalDate.now(), hours);
 	}

	private void saveRegToDatabase(int personId, int projectNr, LocalDate date, double hours) {
		HourRegistration reg = HourRegistration.createRegistration(personId, projectNr, LocalDate.now(), hours);
		db.saveHours(reg);
		System.out.println("Saving registration with data: " + projectNr + "," +hours+ ", " +LocalDate.now());
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		db.beginTransaction();
		super.service(req, resp);
		db.endTransaction(true);
		//TODO sleng p� en finally her s� den ender transaksjonen hvis servleten kr�sjer
	}

}
