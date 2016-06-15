DESCRIPTION = "QCOM Firmware for DragonBoard 410c"

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=003cae816e20ae44589f8f7cc022cb54"

SRC_URI = "http://developer.qualcomm.com/download/db410c/firmware-410c-${PV}.bin;qcom-eula=true"
SRC_URI[md5sum] = "de6038f1c07b93886b8d0845a1d8eb4b"
SRC_URI[sha256sum] = "c017b4c1bc4e52294539ef84ea18ed9a06e863b553c96eb5aaad915fc8b41bd6"

DEPENDS += "mtools-native"

COMPATIBLE_MACHINE = "(dragonboard-410c)"
PACKAGE_ARCH = "${MACHINE_ARCH}"

S = "${WORKDIR}/linux-ubuntu-board-support-package-v1.2"

python qcom_bin_do_unpack() {
    src_uri = (d.getVar('SRC_URI', True) or "").split()
    if len(src_uri) == 0:
        return

    localdata = bb.data.createCopy(d)
    bb.data.update_data(localdata)

    rootdir = localdata.getVar('WORKDIR', True)
    fetcher = bb.fetch2.Fetch(src_uri, localdata)

    for url in fetcher.ud.values():
        save_cwd = os.getcwd()
        # Check for supported fetchers
        if url.type in ['http', 'https', 'ftp', 'file']:
            if url.parm.get('qcom-eula', False):
                # If download has failed, do nothing
                if not os.path.exists(url.localpath):
                    bb.debug(1, "Exiting as '%s' cannot be found" % url.basename)
                    return

                # Change to the working directory
                bb.note("Handling file '%s' as a Qualcomm's EULA binary." % url.basename)
                save_cwd = os.getcwd()
                os.chdir(rootdir)

                cmd = "sh %s --auto-accept --force" % (url.localpath)
                bb.fetch2.runfetchcmd(cmd, d, quiet=True)

    # Return to the previous directory
    os.chdir(save_cwd)
}

python do_unpack() {
    eula = d.getVar('ACCEPT_EULA_dragonboard-410c', True)
    eula_file = d.getVar('QCOM_EULA_FILE', True)
    pkg = d.getVar('PN', True)
    if eula == None:
        bb.fatal("To use '%s' you need to accept the EULA at '%s'. "
                 "Please read it and in case you accept it, write: "
                 "ACCEPT_EULA_dragonboard-410c = \"1\" in your local.conf." % (pkg, eula_file))
    elif eula == '0':
        bb.fatal("To use '%s' you need to accept the EULA." % pkg)
    else:
        bb.note("EULA has been accepted for '%s'" % pkg)

    # The binary unpack needs to be done first so 'S' is valid
    bb.build.exec_func('qcom_bin_do_unpack', d)

    try:
        bb.build.exec_func('base_do_unpack', d)
    except:
        raise
}

do_compile() {
	:
}

do_install() {
    install -d  ${D}/lib/firmware/
    rm -f ./proprietary-ubuntu/firmware.tar
    cp -r ./proprietary-ubuntu/* ${D}/lib/firmware/

    MTOOLS_SKIP_CHECK=1 mcopy -i ./bootloaders-ubuntu/NON-HLOS.bin \
    ::image/modem.* ::image/mba.mbn ${D}/lib/firmware/

    install -d ${D}${sysconfdir}/
    install -m 0644 LICENSE ${D}${sysconfdir}/license.txt
}

FILES_${PN} += "/lib/firmware/*"
INSANE_SKIP_${PN} += "arch"
