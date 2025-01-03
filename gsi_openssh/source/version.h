/* $OpenBSD: version.h,v 1.97 2023/03/15 21:19:57 djm Exp $ */

#define SSH_VERSION	"OpenSSH_9.3"

#ifdef GSI
#define GSI_VERSION	" GSI"
#else
#define GSI_VERSION	""
#endif

#ifdef KRB5
#define KRB5_VERSION	" KRB5"
#else
#define KRB5_VERSION	""
#endif

#define SSH_PORTABLE	"p1"
#define GSI_PORTABLE	"c-GSI"
#define SSH_HPN		"-hpn15v2"
#define SSH_RELEASE	SSH_VERSION SSH_PORTABLE GSI_PORTABLE \
			GSI_VERSION SSH_HPN KRB5_VERSION
