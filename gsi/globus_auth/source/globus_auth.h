/*
 * This file is a mapping between the higher level authorization API
 * for the ftp daemon and the gaa api definitions.
 */
 
#ifndef __GLOBUS_AUTH_H_
#define __GLOBUS_AUTH_H_

#include "gaa.h"
#include "gaa_core.h"
#include "globus_auth_error.h"
#include "gssapi.h"
#include "gaa_simple.h"

#ifdef _HAVE_GSI_EXTENDED_GSSAPI
#include "globus_gss_ext_compat.h"
#endif

extern
const gss_OID_desc * const gss_mech_globus_gssapi_ssleay;
 
extern
const gss_OID_desc * const gss_restrictions_extension;; 

struct globus_authorization_struct
{
    /* GAA pointer */
    gaa_ptr                             gaa; 
    /* GAA security context */
    gaa_sc_ptr                          gaa_sc; 
    /* GSS security context. Used as raw credentials for gaa_new_cred */
    gss_ctx_id_t                        gss_context;
    /* The config file from which policy info is parsed: */
    gaa_policy_ptr                      policy;         
    gaa_string_data                     policy_source;
    gaa_simple_callback_arg_t           gaa_cb_arg;   
#ifdef DEBUG
    gaa_answer_ptr                      debug_answer;
#endif
};

typedef struct globus_authorization_struct 
    globus_authorization_t, *globus_authorization_handle_t;
 
globus_auth_result_t
globus_authorization_handle_init(
    globus_authorization_handle_t *     handle, 
    char *                              configfile,
    char *                              actions[],
    char *                              urlbase,
    char *                              service_type);

globus_auth_result_t
globus_authorization_handle_destroy(
	globus_authorization_handle_t * handle);

globus_auth_result_t
globus_authorization_handle_set_policy_source(
    globus_authorization_handle_t       handle,
    char *                              policy_source);


globus_auth_result_t
globus_authorization_handle_get_policy_source(
    globus_authorization_handle_t       handle,
    char *                              policy_source,
    int *                               length);

globus_auth_result_t
globus_authorization_handle_set_gss_ctx(
    globus_authorization_handle_t       handle,
    gss_ctx_id_t                        context);

globus_auth_result_t
globus_authorization_handle_get_gss_ctx(
    globus_authorization_handle_t       handle,
    gss_ctx_id_t *                      context);

globus_auth_result_t
globus_authorization_handle_get_local_identity(
    globus_authorization_handle_t       handle,
    char *                              local_identity,
    int *                               length);

/*globus_auth_result_t
globus_authorization_handle_add_service_type(
    globus_authorization_handle_t handle,
    char * service);*/

globus_auth_result_t
globus_authorization_eval(
    globus_authorization_handle_t       handle,
    char *                              object, 
    char *                              service_type, 
    char *                              action);      
 
#define GLOBUS_AUTH_DEFAULT_CONFIG_FILE "/etc/grid-security/globus_gaa.conf"

#endif  /* __GLOBUS_AUTH_H_ */
