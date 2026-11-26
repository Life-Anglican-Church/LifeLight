# GitHub access for development and releases

Publishing uses the LifeACIsaac account to access Life-Anglican-Church organisation repositories through Git Credential Manager and Windows Credential Manager. Sign in with:

```powershell
git credential-manager github login --username LifeACIsaac --browser
```

Complete GitHub's normal authorization in your browser. The prior LifeAnglicanChurch credential-cache alias is also supported after the account rename. Do not paste a password or access token into chat. The release scripts obtain the managed credential in memory; they do not print it or copy it into a project file. They refuse to publish if the account or GitHub host does not match. The publishing token previously saved in `.release-secrets/publish.txt` is no longer read by the current publishing/upload/verification scripts; it has not been deleted or revoked.

The Codex GitHub connection is separate from the Windows-managed login and can have a narrower repository selection. A connected account alone does not prove access to a private repository.

The installed updater has its own existing read-only release credential. It remains separate so church PCs can download private updates without inheriting development/publishing access. This change does not rebuild an installer or rotate that credential.
