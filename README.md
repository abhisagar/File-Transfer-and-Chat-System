# File-Transfer-and-Chat-System
A client and server application to upload files to server, create groups and share messages/files in groups

To compile:
javac Server.java
javac Client.java
javac ClientListener.java
javac ReceivedMessageHandler.java
javac Chatroom.java

To run:
java Server
java Client

Commands that can be run through Client:

create_user `username` -> to login (without login nothing works)

upload `filename` -> for tcp upload from client to server
upload_udp `filename` -> for udp upload from client to server

create_folder `foldername` -> create folder in ServerDir
move_file `source_path` `dest_path`

create_group `groupname` -> create a chatroom
list_groups -> list all available groups

join_group `groupname`
leave_group `groupname`

list_detail `groupname` -> List users and user files with path in a group

share_msg `group name` `message text` -> message shared within group to all members

get_file `file_path` -> download file from ServerDir to client
