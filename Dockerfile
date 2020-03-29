FROM ubuntu

COPY ./out/server/nativeImage/dest/out /opt/server

ENTRYPOINT ["/opt/server"]
