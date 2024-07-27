REM ##############################################################################################################
REM # S T A R T                                                                                                  #
REM # The following lines are being prepended to this bat file as a way of identifying the user and time of file #
REM # execution. On run of this bat file, a RoscoeStart-Info.log #
REM # file will be created/updated for the purpose of logging this information.                                  #
REM ##############################################################################################################
@echo off
set batName=RoscoeStart
whoami > whoami.txt
set datestr=%date:~3,3%-%date:~7,2%-%date:~10,4%
set timef=%time:~0,2%:%time:~3,2%:%time:~6,2%
set /P user=< whoami.txt
del whoami.txt
echo -------------------------- >> %batName%-Info.log
echo Executing %batName%
echo Date: %datestr% %timef% >> %batName%-Info.log
echo User:  %user% >> %batName%-Info.log
echo on
REM ###########
REM # E N D   #
REM ###########
java -jar @filename@
