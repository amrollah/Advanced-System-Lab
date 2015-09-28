mkdir D:\ETH\Fall2014\ASL\last_logs\%1\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\c1_log\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\m1_log\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\c2_log\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\m2_log\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\c3_log\
mkdir D:\ETH\Fall2014\ASL\last_logs\%1\m3_log\

winscp.com /script=download_logs.txt /parameter %1
winscp.com /script=download_logs2.txt /parameter %1
winscp.com /script=download_logs3.txt /parameter %1
cd ..\
ant -Dlog_dir=D:\ETH\Fall2014\ASL\last_logs\%1\ log_analytics