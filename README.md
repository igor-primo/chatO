# chatO

chatO is a simple Java written CLI chat that uses AMQP protocol and Protocol Buffers as 
data serialization.

If you have already a RabbitMQ server running, you can login into the application thus:

`java -jar target/<jar_name_of_app> <server_ip> <user_name> <password>`

Then you will be presented with a prompt. To talk to a user you can write:

`@another_user_name`

Then the prompt will change to indicate that messages passed to it will
be sent to 'another_user_name'.

You can create a group by writing:

`!addGroup <name_of_group>`

And you can add users to it by writing:

`!addUser <name_of_user> <name_of_group>`

You log into the group thus:

`#<name_of_group>

And the prompt will change to indicate that messages passed to it will be sent to all users
in that group.

See the list of groups you are in:

`!listGroups`

See the list of users in a group:

`!listUsers <name_of_group>`

To remove someone from a group:

`!delFromGroup <name_of_user> <name_of_group>`

To delete a group:

`!removeGroup <name_of_group>`

To upload a file:

`!upload <path_of_file>`
