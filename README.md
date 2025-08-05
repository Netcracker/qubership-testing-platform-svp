# Qubership Testing Platform SVP (Single Validation Page)

## Description

**SVP (Single Validation Page)** is a service designed to validate information collected from various sources, including:

- SQL databases (Cassandra, MySQL, PostgreSQL, Oracle)
- REST and SOAP APIs
- SSH connections

The service supports data processing and transformation, such as converting JSON into a tabular format. Once all necessary data is collected and processed, it can be exported as a preformatted Microsoft Word report.

**SVP** also integrates with other QSTP services, such as:

- **BV (Bulk Validator)** — for bulk data validation
- **Log Collector** — for log collection and analysis

## How to start Backend

(in some case with flag -DskipTests)

1. Main class: org.qubership.atp.svp.Main
2. VM options: 
```properties
-Dspring.config.location=target\config\application.properties
-Dsvp.projects.config.path=src/main/config/project/projects_configs_for_test.json
-DFEIGN_ATP_ENVIRONMENTS_URL=http://environments:8080
-Dsvp.session.lifespan=36000
```
3. Working dir (for example): C:\QSTP-SVP\qstp-svp\qstp-svp-backend
4. Use classpath or module: atp-svp-backend
4. Edit application.properties:
```properties
spring.resources.static-locations=file:./atp-svp-backend/web/
server.port=8080
logging.level.org.qubership.atp.ui.project.template=INFO
```

Just run Main#main with args from step above

# How to deploy tool

1. Build snaphot (artifacts and docker image) of https://github.com/Netcracker/qubership-testing-platform-svp in GitHub
2. Clone repository to a place, available from your openshift/kubernetes where you need to deploy the tool to
3. Navigate to <repository-root>/deployments/charts/atp-svp folder
4. Check/change configuration parameters in the ./values.yaml file according to your services installed
5. Execute the command: helm install qstp-svp
6. After installation is completed, check deployment health
