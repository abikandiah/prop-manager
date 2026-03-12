package com.akandiah.propmanager.security;

import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** Writes a ProblemDetail JSON body directly to a servlet response (for use in filters). */
public final class FilterProblemResponse {

	private FilterProblemResponse() {}

	public static void write(HttpServletResponse response, ProblemDetail pd, ObjectMapper mapper)
			throws IOException {
		if (response.isCommitted()) return;
		response.setStatus(pd.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		mapper.writeValue(response.getOutputStream(), pd);
	}
}
