package com.fas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfSystemProperty(
		named = "fas.test.db.url",
		matches = "jdbc:mysql://localhost:3306/fas_verify_test(?:\\?.*)?")
@SpringBootTest(properties = {
		"spring.datasource.url=${fas.test.db.url}",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.show-sql=false",
		"fas.seed.administrator.password=test-admin-password",
		"fas.seed.hod.password=test-hod-password",
		"fas.seed.faculty.password=test-faculty-password"
})
class FasApplicationTests {

	@Test
	void contextLoads() {
	}

}
