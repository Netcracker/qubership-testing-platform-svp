FROM artifactory-service-address/path-to-java-image

LABEL maintainer="example@example.com"
LABEL qubership.atp.service="atp-svp"

ENV HOME_EX=/atp-svp
WORKDIR $HOME_EX

COPY --chmod=775 dist/atp /atp/
COPY --chown=atp:root build $HOME_EX/

RUN apk add --update --no-cache subversion && \
    find $HOME_EX -type f -exec chmod a+x {} + && \
    find $HOME_EX -type d -exec chmod 777 {} \;

EXPOSE 8080 9000

USER atp

CMD ["./run.sh"]
