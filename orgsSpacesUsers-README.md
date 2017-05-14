# Orgs, Spaces and users

`cf help -a`

```
ORGS:
   orgs                                   List all orgs
   org                                    Show org info

   create-org                             Create an org
   delete-org                             Delete an org
   rename-org                             Rename an org

SPACES:
   spaces                                 List all spaces in an org
   space                                  Show space info

   create-space                           Create a space
   delete-space                           Delete a space
   rename-space                           Rename a space

   allow-space-ssh                        Allow SSH access for the space
   disallow-space-ssh                     Disallow SSH access for the space
   space-ssh-allowed                      Reports whether SSH is allowed in a space

USER ADMIN:
   create-user                            Create a new user
   delete-user                            Delete a user

   org-users                              Show org users by role
   set-org-role                           Assign an org role to a user
   unset-org-role                         Remove an org role from a user

   space-users                            Show space users by role
   set-space-role                         Assign a space role to a user
   unset-space-role                       Remove a space role from a user

ORG ADMIN:
   quotas                                 List available usage quotas
   quota                                  Show quota info
   set-quota                              Assign a quota to an org

   create-quota                           Define a new resource quota
   delete-quota                           Delete a quota
   update-quota                           Update an existing resource quota

   share-private-domain                   Share a private domain with an org
   unshare-private-domain                 Unshare a private domain with an org

SPACE ADMIN:
   space-quotas                           List available space resource quotas
   space-quota                            Show space quota info

   create-space-quota                     Define a new space resource quota
   update-space-quota                     Update an existing space quota
   delete-space-quota                     Delete a space quota definition and unassign the space quota from all spaces

   set-space-quota                        Assign a space quota definition to a space
   unset-space-quota
```

## login with a user:
`cf login -a api.local.pcfdev.io --skip-ssl-validation -u user -p pass`

## check where we are working on:
`cf target`

## what organizations i have access to:
`cf orgs`

## check that organization details: domains, quotas, and available spaces
`cf org engineering`

> Administrators can manage quotas and assign them to org and spaces

## lets check the space (`cf spaces`) details: it tells us which apps, domains, quotas, services and security groups
`cf space demo`

 > security groups not used at solera but it is a like outward firewall: `all_open`  is the default
 ```
 Rules
	[
		{
			"destination": "0.0.0.0-255.255.255.255",
			"protocol": "all"
		}
	]
```

## what we can do with this user? push apps, create services.
we cannot `create-org` or `create-space` or assign privileges to other users.

## Lets login as an administrator user: (`cf logout` first)
`cf login -a api.local.pcfdev.io --skip-ssl-validation -u admin -p admin`

## Lets create a user for the person who will be responsible for an entire team1/solution
`cf create-user team1Manager pwd`

## Now we need an organization for team1
`cf create-org team1`

> it automatically assigns the current user as OrgManager of team1 which is not want we want though

## Now want to grant permission to team1Manager to manage the team1 org,  i.e., so that team1Manager can create users.
`cf set-org-role team1Manager team1 OrgManager`

> There are 2 more roles associated to organizations which are not that important : BillingManager, OrgAuditor

## Login now as team1Manager
`cf login -a api.local.pcfdev.io --skip-ssl-validation -u team1Manager -p pwd`

It is going to print out this message
```
API endpoint:   https://api.local.pcfdev.io (API version: 2.65.0)
User:           team1Manager
Org:            team1
Space:          No space targeted, use 'cf target -s SPACE'
```

We have not created any space yet. It is the job of the OrgManager to create the spaces needed by the team.

## Create the development space
`cf create-space dev`

> WE could have specified the org also but by default it will use the organization we are targeting which is `team1`

Look at the output from this command:
```
 ...
 Assigning role RoleSpaceManager to user team1Manager in org team1 / space dev as team1Manager...
  OK
  Assigning role RoleSpaceDeveloper to user team1Manager in org team1 / space dev as team1Manager...
  OK
```

`team1Manager` is automatically assigned 2 roles: `RoleSpaceManager` and `RoleSpaceDeveloper`.

## Create users for the team members
`team1Manager` can invite team members to join the organization `team1` but cannot create-users directly.

> To go around this, the admin user will create the users and assign them to `team1` org

`cf login -a api.local.pcfdev.io --skip-ssl-validation -u admin -p admin -o team1`

Create `bob` user:
`cf create-user bob pwd`

And assign it to the space `dev` in `team1` as `SpaceDeveloper`:
`cf set-space-role bob team1 dev SpaceDeveloper`

Let's see who are the `team1` users. We are not going to be looking at all users but only users which have any type of Organization role, be it OrgManager, or BillingManager or OrgAuditor.
`cf org-users team1`

  It turns out we have:
  ```
  ORG MANAGER
  admin
  team1Manager
  ```

What about users in `dev` space?
`cf space-users team1 dev`

  It turns out we have the `team1Manager` and now `bob`. But `bob` can only push/manage apps, manage services but it cannot invite users to the space.
  ```
  SPACE MANAGER
  team1Manager

  SPACE DEVELOPER
  team1Manager
  bob
  ```

## team1Manager grant more permissions to team members
`team1Manager` wants to let `bob` to also manager the team so that he can also invite users and manage their roles.

First login as `team1Manager`
`cf login -a api.local.pcfdev.io --skip-ssl-validation -u team1Manager -p pwd`

Grant `OrgManager` role also to `bob`:
`cf set-org-role bob team1 OrgManager`

We check the users in `team1` org and `bob` also appears as `OrgManager`:
```
ORG MANAGER
  admin
  team1Manager
  bob
```

## who can push, start/stop application?
A SpaceDeveloper can push apps and start/stop them. A user who is only SpaceManager or an OrgManager cannot push applications.

# Organization of spaces and orgs in real life

One possible organization is that each line of business or solution which is managed by a team of people, that team gets assigned an organization. Within that organization there are a number of people who administers the team so that they can create users, spaces, etc.

For each runtime environment, we create a space. e.g. development, production, UAT, demo, etc.

Typically, each developer will have its own user and assigned to the `development` space so that they can push apps, start/stop them, delete them, etc.

For the rest of the spaces, like production, or UAT, we should aim for automated deployments where no-human interacts with PCF to deploy applications. Instead it is all done thru a CI tool like *Concourse*.

Each environment may have its own deployment pipeline and its own user. This user only has access to the environment's space and has only the `SpaceDeveloper` role (least privilege security model. If this user gets compromised, it can only deploy to this space and not others).
