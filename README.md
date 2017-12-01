# Data & Analytics Framework (DAF)

Welcome to the project homepage.

* [Documentation for developers](https://developers.italia.it/it/daf/#documentazione)
* [User Manual](docs-usr)


## Development Guidelines

In general, we prefer to keep all the project with Version 1.0.0-SNAPSHOT. This requires that could need to publish to your local nexus ivy repository some local projects.
This part is experimental and can change in the future.

### Internal Team

Each time you start working on the DAF the desiderata is that:

- For a new feature you have to create a branch with a meaningful name. The desiderata is something like `feature_some_meaningful_name`. It would be useful also to have a branch related to the feature
- For a bug-fix you have to create a branch named `bug_number_of_the_bug`

Whenever the work on the branch is finished it is need to:

1. squash all your commit in one commit
2. create a pull request for master and assign it to another one in the team.

If you don't have practice with branching, squashing and merging you can use [git-extras](https://github.com/tj/git-extras) as helper. Git extras has commands like: `git feature` to create a feature, `git squash` to squash your commits.

The aim of this is to share your work.

The releases will be tagged and there will be also a branch.

### External TEAM

Please fork the project and then do a pull request at the end. Pull request are super welcome !!! :)
