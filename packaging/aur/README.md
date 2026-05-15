# DarkNote AUR Package

This directory contains the PKGBUILD for distributing DarkNote on Arch Linux via the AUR (Arch User Repository).

## Package Information

- **Package name:** `darknote`
- **Description:** Modern snippet manager with Dropbox sync - Desktop app for Linux
- **Architecture:** x86_64
- **License:** GPL-3.0

## Dependencies

### Required
- `java-runtime>=17` - Java 17+ runtime environment
- `sqlite` - SQLite database engine

### Optional
- `plasma-desktop` - For KDE Plasma integration
- `breeze` - For native theme support

## Building from Source

### Prerequisites
```bash
sudo pacman -S jdk17-openjdk gradle
```

### Build the package
```bash
cd packaging/aur
makepkg -si
```

This will:
1. Download the source tarball
2. Build the desktop JAR using Gradle
3. Run tests
4. Install the application system-wide

## Installation from AUR

Once published to AUR:

### Using yay
```bash
yay -S darknote
```

### Using paru
```bash
paru -S darknote
```

### Manual installation
```bash
git clone https://aur.archlinux.org/darknote.git
cd darknote
makepkg -si
```

## Post-Installation

After installation, you can:

1. **Launch from Application Menu**
   - Find "DarkNote" in your application launcher
   - Category: Utility

2. **Launch from Terminal**
   ```bash
   darknote
   ```

3. **KDE Plasma Users**
   - The app will automatically detect and use Breeze theme
   - Desktop actions available in launcher context menu
   - Searchable via Krunner

## File Locations

- **Binary:** `/usr/bin/darknote`
- **JAR:** `/usr/share/java/darknote/darknote.jar`
- **Desktop Entry:** `/usr/share/applications/darknote.desktop`
- **Icons:** `/usr/share/icons/hicolor/{48,128,256,512}x{48,128,256,512}/apps/darknote.png`
- **Documentation:** `/usr/share/doc/darknote/`
- **License:** `/usr/share/licenses/darknote/LICENSE`

## Uninstallation

### Using package manager
```bash
sudo pacman -R darknote
```

### Remove user data (optional)
```bash
rm -rf ~/.local/share/darknote
rm -rf ~/.config/darknote
```

## Publishing to AUR

### First-time Setup

1. **Create AUR account**
   - Visit https://aur.archlinux.org/register

2. **Set up SSH key**
   ```bash
   ssh-keygen -t ed25519 -C "your_email@example.com"
   cat ~/.ssh/id_ed25519.pub
   ```
   - Add the public key to your AUR account settings

3. **Clone AUR repository**
   ```bash
   git clone ssh://aur@aur.archlinux.org/darknote.git aur-darknote
   cd aur-darknote
   ```

4. **Copy files**
   ```bash
   cp ../packaging/aur/PKGBUILD .
   cp ../packaging/aur/.SRCINFO .
   ```

5. **Update checksums**
   ```bash
   # After uploading the release tarball
   updpkgsums
   makepkg --printsrcinfo > .SRCINFO
   ```

6. **Commit and push**
   ```bash
   git add PKGBUILD .SRCINFO
   git commit -m "Initial commit: darknote 1.0.0"
   git push
   ```

### Updating the Package

1. **Update version in PKGBUILD**
   ```bash
   pkgver=1.1.0
   pkgrel=1
   ```

2. **Update checksums**
   ```bash
   updpkgsums
   makepkg --printsrcinfo > .SRCINFO
   ```

3. **Test the build**
   ```bash
   makepkg -f
   ```

4. **Commit and push**
   ```bash
   git add PKGBUILD .SRCINFO
   git commit -m "Update to 1.1.0"
   git push
   ```

## Testing

### Test the PKGBUILD locally
```bash
# Clean previous builds
rm -rf src/ pkg/ *.tar.gz

# Build
makepkg -f

# Install locally
sudo pacman -U darknote-1.0.0-1-x86_64.pkg.tar.zst

# Test the app
darknote

# Uninstall
sudo pacman -R darknote
```

### Test in clean chroot
```bash
# Install devtools
sudo pacman -S devtools

# Create chroot
mkdir -p ~/chroot
mkarchroot ~/chroot/root base-devel

# Build in chroot
makechrootpkg -c -r ~/chroot
```

## Troubleshooting

### Build fails with "gradlew not found"
Ensure the source tarball includes the gradle wrapper:
```bash
tar -tzf darknote-1.0.0.tar.gz | grep gradlew
```

### Java version mismatch
Update your Java:
```bash
sudo pacman -S jdk17-openjdk
sudo archlinux-java set java-17-openjdk
```

### Tests fail
You can skip tests temporarily (not recommended for AUR):
```bash
makepkg --nocheck
```

## Maintainer Notes

### Before publishing
- [ ] Update version in PKGBUILD
- [ ] Create and upload GitHub release tarball
- [ ] Update sha256sums
- [ ] Test build locally
- [ ] Test in clean chroot
- [ ] Update .SRCINFO
- [ ] Test installation
- [ ] Push to AUR

### Release checklist
1. Tag release in git: `git tag v1.0.0 && git push --tags`
2. Create GitHub release with tarball
3. Get sha256: `sha256sum darknote-1.0.0.tar.gz`
4. Update PKGBUILD checksums
5. Test build: `makepkg -f`
6. Update .SRCINFO: `makepkg --printsrcinfo > .SRCINFO`
7. Push to AUR

## Links

- **Project Repository:** https://github.com/yourusername/darknote
- **AUR Package:** https://aur.archlinux.org/packages/darknote
- **Issue Tracker:** https://github.com/yourusername/darknote/issues
- **Wiki:** https://github.com/yourusername/darknote/wiki

## License

This PKGBUILD is released under the same license as DarkNote (GPL-3.0).
