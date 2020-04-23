package software.marshallapps.dqldeveloper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class DQLDeveloperApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(DQLDeveloperApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(DQLDeveloperApplication.class, args);
	}

}
