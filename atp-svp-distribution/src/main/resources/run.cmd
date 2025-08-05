java -cp "config/;lib/*" -Dsvp.projects.config.path=./config/project ^
-Dsvp.projects.config.name=projects_configs.json ^
-DFEIGN_ATP_ENVIRONMENTS_URL=http://environments:8080 ^
-DFEIGN_ATP_LOGCOLLECTOR_URL=http://log-collector:8080 ^
-Dlog.graylog.on=false ^
-Dlog.graylog.host=tcp:graylog.somedomain.com ^
-Dlog.graylog.port=12201 ^
-Dlogging.config=./config/logback.xml ^
-Dlog.level=INFO ^
-Dkafka.logcollector.event.enable=false ^
-Dspring.cloud.vault.enabled=false ^
-Dspring.cloud.consul.config.enabled=false ^
-Dsvp.deferred-search-results.lifespan.sec=600 ^
-Dsvp.session.lifespan=1800 ^
-DXms256m ^
-DXmx1600m ^
org.qubership.atp.svp.Main
pause
