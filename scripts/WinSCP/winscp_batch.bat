mkdir D:\ETH\Fall2014\ASL\last_logs\%1\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\c1_log\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\m1_log\

winscp.com /script=download_logs.txt /parameter %1
cd ..\
ant -Dlog_dir=D:\ETH\Fall2014\ASL\last_logs\%1\ log_analytics