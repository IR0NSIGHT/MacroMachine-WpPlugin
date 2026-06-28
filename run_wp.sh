#!/bin/bash
mvn test -PtestWithWorldpainter -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
