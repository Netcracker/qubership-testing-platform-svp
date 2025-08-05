#!/usr/bin/env sh

xargs -rt -a /atp-svp/application.pid kill -SIGTERM
sleep 29
