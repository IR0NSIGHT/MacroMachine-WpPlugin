#!/bin/bash
mvn package # complete build first (for frontend)
echo "completed build, start"
mvn test -PtestWithWorldPainter -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
