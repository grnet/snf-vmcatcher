create database snfvmcatcher;
create user vmcatcher@localhost identified by 'PASSWORD';
grant all privileges on snfvmcatcher.* to vmcatcher@localhost;
flush privileges;

-- You can connect from the command-line tool using:
-- ------------------
--   $ mysql -u vmcatcher -pPASSWORD snfvmcatcher
-- ------------------
