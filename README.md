# chatO

chatO is a simple Java written CLI chat that uses AMQP protocol and Protocol Buffers as 
data serialization.

If you have already a RabbitMQ server running, you can login into the application thus:

`java -jar target/\<jar\_name\_of\_app\> \<server\_ip\> \<user\_name\> \<password\>`

Then you will be presented with a prompt. To talk to a user you can write:

`@another\_user\_name`

Then the prompt will change to indicate that messages passed to it will
be sent to 'another\_user\_name'.

You can create a group by writing:

`!addGroup \<name\_of\_group\>`

And you can add users to it by writing:

`!addUser \<name\_of\_user\> \<name\_of\_group\>`

You log into the group thus:

`#\<name\_of\_group\>

And the prompt will change to indicate that messages passed to it will be sent to all users
in that group.

See the list of groups you are in:

`!listGroups`

See the list of users in a group:

`!listUsers \<name\_of\_group\>`

To remove someone from a group:

`!delFromGroup \<name\_of\_user\> \<name\_of\_group\>`

To delete a group:

`!removeGroup \<name\_of\_group\>`

To upload a file:

`!upload \<path\_of\_file\>`
