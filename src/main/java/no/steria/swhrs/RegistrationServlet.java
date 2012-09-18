package no.steria.swhrs;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.simple.JSONObject;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * @author xsts
 *
 */
public class RegistrationServlet extends HttpServlet {
	private static final long serialVersionUID = -1090477374982937503L;
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_TEXT = "application/text";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_JSON = "text/json";

	private HourRegDao db;

	LocalDate date = LocalDate.now();

    @Override
    public void init() throws ServletException {
        try {
            db = MSSQLHourRegDao.createInstance();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getRequestURL().toString().contains("hours/daylist")){
            getDaylistResponseAsJSON(req, resp);
        } else if(req.getRequestURL().toString().contains("hours/favourite")){
            getFavorites(req, resp);
        } else if(req.getRequestURL().toString().contains("hours/week")){
            getWeeklistResponseAsJSON(req, resp);
        } else if(req.getRequestURL().toString().contains("hours/searchFavourites")){
            searchFavourites(req, resp);
        }
    }

    @Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
        if (req.getRequestURL().toString().contains("hours/registration")) {
            addHourRegistration(req);
        } else if(req.getRequestURL().toString().contains("hours/deleteRegistration")){
            deleteHourRegistration(req, resp);
		} else if(req.getRequestURL().toString().contains("hours/addFavourites")){
			addFavourites(req);
		} else if(req.getRequestURL().toString().contains("hours/submitPeriod")){
			submitPeriod(req, resp);
		} else if(req.getRequestURL().toString().contains("hours/reopenPeriod")){
            reopenPeriod(req, resp);
        } else if(req.getRequestURL().toString().contains("hours/deleteFavourite")){
			deleteFavourite(req);
		} else if(req.getRequestURL().toString().contains("hours/updateRegistration")){
			updateHourRegistration(req);
		}
 	}

    /**
     * Adds an hour registration the database
     * @param req The HTTP request containing parameters of "ProjectNr", "hours", "lunchNumber" and "description"
     */
    private void addHourRegistration(HttpServletRequest req) {
        User user = getUserAttribute(req);
        String username = user.getUsername();
        String projectNumber = req.getParameter(RegistrationConstants.PROJECT_NUMBER);
        String activityCode = req.getParameter(RegistrationConstants.ACTIVITY_CODE);
        double hours = Double.parseDouble(req.getParameter(RegistrationConstants.HOURS));
        String description = req.getParameter(RegistrationConstants.DESCRIPTION);
        boolean billable = StringUtils.equals("1", req.getParameter(RegistrationConstants.BILLABLE));
        DateTime date = new DateTime(req.getParameter(RegistrationConstants.DATE));
        String workType = req.getParameter(RegistrationConstants.WORK_TYPE);

        db.addHourRegistrations(username, username, projectNumber, activityCode, date, hours, billable,
                workType, description, false);
    }

	/**
	 * This method updates an hour registration with a new description for time entry (comment) and new hours number
	 * @param req HttpServletRequest
	 */
	private void updateHourRegistration(HttpServletRequest req) {
        User user = getUserAttribute(req);
        String userId = user.getUsername();
		String taskNumber = req.getParameter(RegistrationConstants.TASK_NUMBER);
		double hours = Double.parseDouble(req.getParameter(RegistrationConstants.HOURS));
		String description = req.getParameter(RegistrationConstants.DESCRIPTION);
        String activity = req.getParameter(RegistrationConstants.ACTIVITY_CODE);
        String projectNumber = req.getParameter(RegistrationConstants.PROJECT_NUMBER);
        DateTime date = new DateTime(req.getParameter(RegistrationConstants.DATE));
        boolean billable = StringUtils.equals("1", req.getParameter(RegistrationConstants.BILLABLE));
        String workType = req.getParameter(RegistrationConstants.WORK_TYPE);

        db.updateHourRegistration(userId, taskNumber, projectNumber, activity, date, hours, billable, workType, description);
	}

    /**
     * This method will delete an hour registration from the database
     * @param req The HTTP request contains taskNumber which is the unique identifier for each registration in the database
     * @param resp The HTTP response will return plain text with either
     * 			   "ERROR: Already submitted" if the deleteHourRegistration returns false, meaning that the registration is locked
     * 			or "success" if the deletion was successful
     * @throws IOException
     */
    private void deleteHourRegistration(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getUserAttribute(req);
        String username = user.getUsername();
        String taskNumber = req.getParameter(RegistrationConstants.TASK_NUMBER);
        db.deleteHourRegistration(username, taskNumber);

        resp.setContentType(TEXT_PLAIN);
        resp.setStatus(RegistrationConstants.HTTP_STATUS_CODE_SUCCESS);
        resp.getWriter().append(RegistrationConstants.TEXT_SUCCESS);
    }

	/**
	 * This method removes a project from the users favourite list in the database
	 * @param req The HTTP request contains project number and activity code
	 */
	private void deleteFavourite(HttpServletRequest req){
        User user = getUserAttribute(req);
        String projectNumber = req.getParameter(RegistrationConstants.PROJECT_NUMBER);
		String activityCode = req.getParameter(RegistrationConstants.ACTIVITY_CODE);
		db.deleteFavourite(user.getUsername(), projectNumber, activityCode);
	}

	/**
	 * This method adds new projects to the users favourite list
	 * @param req The HTTP request contains project number and activity code
	 */
	private void addFavourites(HttpServletRequest req) {
        User user = getUserAttribute(req);
        String projectNumber = req.getParameter(RegistrationConstants.PROJECT_NUMBER);
        String activityCode = req.getParameter(RegistrationConstants.ACTIVITY_CODE);
		db.addFavourites(user.getUsername(), projectNumber, activityCode);
	}

    /**
     * Returns a HTTP response containing a JSON object of the users favourites stored in the database
     * @param resp HTTP request containing a JSON object of the users favourites
     * @throws IOException
     */
    private void getFavorites(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(APPLICATION_JSON);
        User user = getUserAttribute(req);
        List<UserFavourites> userList = db.getUserFavourites(user.getUsername());

        PrintWriter writer = resp.getWriter();
        writer.append(JSONBuilder.createFromFavourites(userList).toString());
        resp.setStatus(RegistrationConstants.HTTP_STATUS_CODE_SUCCESS);
    }

    /**
	 * This method will search through Projects with the search input, and 
	 * return a JSON string with project number, activity code and description
	 * @param req The HTTP request contains a search input which is used to search through
	 * projects in the database
	 * @param resp THE HTTP response will return a JSON String with project number, activity code, and description.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void searchFavourites(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType(APPLICATION_JSON);
		String searchInput = req.getParameter("search");
		List<Projects> project = db.searchProjects(searchInput);

		PrintWriter writer = resp.getWriter();
		writer.append(JSONBuilder.createProjects(project).toString());
        resp.setStatus(RegistrationConstants.HTTP_STATUS_CODE_SUCCESS);
	}
	
	/**
	 * This method will submit a period entry - start date of the period needs to be passed as an argument.
	 * @param req The HTTP request
	 * @param resp The HTTP response
	 * @throws IOException
	 */
	private void submitPeriod(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = getUserAttribute(req).getUsername();
        DateTime startDate = new DateTime(req.getParameter(RegistrationConstants.DATE));
        db.submitHours(username, username, startDate);
        resp.setStatus(RegistrationConstants.HTTP_STATUS_CODE_SUCCESS);
    }

    /**
     * This method will attempt to reopen a period entry - start date of the period needs to be passed as an argument.
     * @param req The HTTP request
     * @param resp The HTTP response
     * @throws IOException
     */
    private void reopenPeriod(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = getUserAttribute(req).getUsername();
        DateTime startDate = new DateTime(req.getParameter(RegistrationConstants.DATE));
        db.reopenHours(username, username, startDate);
        resp.setStatus(RegistrationConstants.HTTP_STATUS_CODE_SUCCESS);
    }
	
	/**
	 * This method returns a HTTP request containing JSON data for a period
	 * @param req The HTTP request contains the week parameter which contains strings of either "thisWeek", "prevWeek" or "nextWeek"
	 * @param resp The HTTP response will return a json object containing data about weekdays, dates and hours for the week requested
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void getWeeklistResponseAsJSON(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(APPLICATION_TEXT);

        User user = getUserAttribute(req);
        String week = req.getParameter("week");

		DatePeriod period2 = db.getPeriod(user.getUsername(), date.toString());
		
		LocalDate localFromDate = new LocalDate(period2.getFromDate().split(" ")[0]);
		LocalDate localToDate = new LocalDate(period2.getToDate().split(" ")[0]);
		
		if(week.equals("nextWeek")) date = localToDate.plusDays(1);
		if(week.equals("prevWeek")) date = localFromDate.minusDays(1);
		
		DatePeriod period = db.getPeriod(user.getUsername(), date.toString());
		
		LocalDate localFromDate2 = new LocalDate(period.getFromDate().split(" ")[0]);
		LocalDate localToDate2 = new LocalDate(period.getToDate().split(" ")[0]);
		
		ArrayList<String> dateArray = new ArrayList<String>();
		while(localFromDate2.compareTo(localToDate2) <= 0  ){
			dateArray.add(localFromDate2.toString()+":"+localFromDate2.getDayOfWeek());
			localFromDate2 = localFromDate2.plusDays(1);
		}
		
		List<WeekRegistration> weekList = db.getWeekList(user.getUsername(), period.getFromDate(), period.getToDate());
		String weekDescription = period.getDescription();
		
		//This will get the norm time for each day of the week and should be added together with the weekHours.
		//List<NormTime> norm = db.getNormTime(username);
		
		
		JSONObject obj = new JSONObject();
		//JSONObject weekJson = new JSONObject();
		for(int i=0; i<dateArray.size(); i++){
			String dateArr = dateArray.get(i).toString().split(":")[0];
			String dayOfWeek = dateArray.get(i).toString().split(":")[1];
			boolean found = false;
			for(WeekRegistration weekRegistration: weekList){
				if(weekRegistration.getDate().split(" ")[0].equals(dateArr)){
					@SuppressWarnings("rawtypes")
					List list = new LinkedList();
					list.add(dayOfWeek);
					list.add(weekRegistration.getHours());
					list.add(weekRegistration.getApproved());
					obj.put(weekRegistration.getDate().split(" ")[0], list);
					found = true;
					break;
				}
			}
			
			if (!found) {
				@SuppressWarnings("rawtypes")
				List list = new LinkedList();
				list.add(dayOfWeek);
				list.add(0);
				obj.put(dateArr, list);
			}
		}
		
		obj.put("weekNumber", weekDescription);
		obj.put("dateHdr", date.getDayOfWeek()+" "+date.toString());
		resp.setContentType(TEXT_JSON);
		PrintWriter writer = resp.getWriter();
		String jsonText = obj.toJSONString();
		writer.append(jsonText);
	}

	/**
	 * Returns a HTTP response containing all hour registrations for a certain day stored in a JSON object
	 * @param req The HTTP request containing a parameter "day" containing either "today", "prevDay"
	 * @param resp The HTTP response contains a json object with all data about a registration needed to display it in the app
	 * @throws IOException
	 */
	private void getDaylistResponseAsJSON(HttpServletRequest req, HttpServletResponse resp)	throws IOException {
        User user = getUserAttribute(req);
        LocalDate date = new LocalDate(req.getParameter("day"));

		List<HourRegistration> hourRegistrationList = db.getAllHoursForDate(user.getUsername(), date.toString());

		PrintWriter writer = resp.getWriter();
		writer.append(JSONBuilder.createFromHours(hourRegistrationList, date.getDayOfWeek() + " " + date.toString()).toString());
        resp.setStatus(RegistrationConstants.HTTP_STATUS_CODE_SUCCESS);
	}

    private static User getUserAttribute(HttpServletRequest req) {
        return (User) req.getAttribute("user");
    }
}
