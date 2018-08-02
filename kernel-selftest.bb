SUMMARY = "Kernel selftest for Linux"
DESCRIPTION = "Kernel selftest for Linux"
LICENSE = "GPLv2"

LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7 \
"

# for musl libc
SRC_URI_libc-musl += "file://userfaultfd.patch \
                      file://0001-bpf-test_progs.c-add-support-for-musllibc.patch \
"

PACKAGECONFIG ??= "bpf vm"

PACKAGECONFIG[bpf] = ",,elfutils libcap libcap-ng rsync-native,"
PACKAGECONFIG[vm] = ",,,libgcc bash"

do_patch[depends] += "virtual/kernel:do_shared_workdir"

inherit linux-kernel-base kernel-arch

do_populate_lic[depends] += "virtual/kernel:do_patch"

S = "${WORKDIR}/${BP}"

# now we just test bpf and vm
# we will append other kernel selftest in the future
TEST_LIST = "bpf \
             vm \
"

EXTRA_OEMAKE = '\
    CROSS_COMPILE=${TARGET_PREFIX} \
    ARCH=${ARCH} \
    CC="${CC}" \
    AR="${AR}" \
    LD="${LD}" \
'

EXTRA_OEMAKE += "\
    'DESTDIR=${D}' \
"

KERNEL_SELFTEST_SRC ?= "Makefile \
                        include \
                        tools \
                        scripts \
                        arch \
			COPYING \
"

python __anonymous () {
    import re

    var = d.getVar('TARGET_CC_ARCH')
    pattern = '_FORTIFY_SOURCE=[^0]'

    if re.search(pattern, var):
        d.appendVar('TARGET_CC_ARCH', " -O")
}

do_compile() {
    for i in ${TEST_LIST}
    do
        oe_runmake -C ${S}/tools/testing/selftests/${i}
    done
}

do_install() {
    for i in ${TEST_LIST}
    do
        oe_runmake -C ${S}/tools/testing/selftests/${i} INSTALL_PATH=${D}/usr/kernel-selftest/${i} install
    done

    chown root:root  -R ${D}/usr/kernel-selftest
}

do_configure() {
    :
}

do_patch[prefuncs] += "copy_kselftest_source_from_kernel remove_clang_related"
python copy_kselftest_source_from_kernel() {
    sources = (d.getVar("KERNEL_SELFTEST_SRC") or "").split()
    src_dir = d.getVar("STAGING_KERNEL_DIR")
    dest_dir = d.getVar("S")
    bb.utils.mkdirhier(dest_dir)
    for s in sources:
        src = oe.path.join(src_dir, s)
        dest = oe.path.join(dest_dir, s)
        if os.path.isdir(src):
            oe.path.copytree(src, dest)
        else:
            bb.utils.copyfile(src, dest)
}

remove_clang_related() {
	sed -i -e '/test_pkt_access/d' -e '/test_pkt_md_access/d' ${S}/tools/testing/selftests/bpf/Makefile
}

PACKAGE_ARCH = "${MACHINE_ARCH}"

INHIBIT_PACKAGE_DEBUG_SPLIT="1"
FILES_${PN} += "/usr/kernel-selftest"
