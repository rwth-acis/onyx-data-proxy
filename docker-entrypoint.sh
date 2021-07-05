#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.onyxDataProxyService.OnyxDataProxyService.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}

# check mandatory variables
[[ -z "${API_ENABLED}" ]] && \
    echo "Mandatory variable API_ENABLED is not set. Add -e API_ENABLED=true or false to your arguments." && exit 1
[[ -z "${OPAL_USERNAME}" ]] && \
    echo "Mandatory variable OPAL_USERNAME is not set. Add -e OPAL_USERNAME=myuser to your arguments." && exit 1
[[ -z "${OPAL_PASSWORD}" ]] && \
    echo "Mandatory variable OPAL_PASSWORD is not set. Add -e OPAL_PASSWORD=pwd to your arguments." && exit 1
[[ -z "${COURSE_LIST}" ]] && \
    echo "Mandatory variable COURSE_LIST is not set. Add COURSE_LIST to your arguments." && exit 1
[[ -z "${PSEUDONYMIZATION_ENABLED}" ]] && \
    echo "Mandatory variable PSEUDONYMIZATION_ENABLED is not set. Add PSEUDONYMIZATION_ENABLED to your arguments." && exit 1

# optional variables
[[ -z "${SERVICE_PASSPHRASE}" ]] && export SERVICE_PASSPHRASE='onyx'
[[ -z "${DATA_STREAM_PERIOD}" ]] && export DATA_STREAM_PERIOD='30'
[[ -z "${COURSE_ELEMENTS_UPDATE_PERIOD}" ]] && export COURSE_ELEMENTS_UPDATE_PERIOD='60'

# configure service properties
function set_in_service_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${SERVICE_PROPERTY_FILE}
}

set_in_service_config apiEnabled ${API_ENABLED}
set_in_service_config opalUsername ${OPAL_USERNAME}
set_in_service_config opalPassword ${OPAL_PASSWORD}
set_in_service_config courseList ${COURSE_LIST}
set_in_service_config pseudonymizationEnabled ${PSEUDONYMIZATION_ENABLED}
set_in_service_config OPAL_DATA_STREAM_PERIOD ${DATA_STREAM_PERIOD}
set_in_service_config OPAL_COURSE_ELEMENTS_UPDATE_PERIOD ${COURSE_ELEMENTS_UPDATE_PERIOD}

# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi

# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* i5.las2peer.tools.L2pNodeLauncher -s service -p '"${LAS2PEER_PORT} ${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

# it's realistic for different nodes to use different accounts (i.e., to have
# different node operators). this function echos the N-th mnemonic if the
# variable WALLET is set to N. If not, first mnemonic is used
function selectMnemonic {
    declare -a mnemonics=("differ employ cook sport clinic wedding melody column pave stuff oak price" "memory wrist half aunt shrug elbow upper anxiety maximum valve finish stay" "alert sword real code safe divorce firm detect donate cupboard forward other" "pair stem change april else stage resource accident will divert voyage lawn" "lamp elbow happy never cake very weird mix episode either chimney episode" "cool pioneer toe kiwi decline receive stamp write boy border check retire" "obvious lady prize shrimp taste position abstract promote market wink silver proof" "tired office manage bird scheme gorilla siren food abandon mansion field caution" "resemble cattle regret priority hen six century hungry rice grape patch family" "access crazy can job volume utility dial position shaft stadium soccer seven")
    if [[ ${WALLET} =~ ^[0-9]+$ && ${WALLET} -lt ${#mnemonics[@]} ]]; then
    # get N-th mnemonic
        echo "${mnemonics[${WALLET}]}"
    else
        # note: zsh and others use 1-based indexing. this requires bash
        echo "${mnemonics[0]}"
    fi
}

#prepare pastry properties
echo external_address = $(curl -s https://ipinfo.io/ip):${LAS2PEER_PORT} > etc/pastry.properties

# start the service within a las2peer node
if [[ -z "${@}" ]]
then
    if [ -n "$LAS2PEER_ETH_HOST" ]; then
        exec ${LAUNCH_COMMAND} --node-id-seed $NODE_ID_SEED --observer --ethereum-mnemonic "$(selectMnemonic)" uploadStartupDirectory startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) startWebConnector "node=getNodeAsEthereumNode()" "registry=node.getRegistryClient()" "n=getNodeAsEthereumNode()" "r=n.getRegistryClient()" 
    else
        exec ${LAUNCH_COMMAND} --node-id-seed $NODE_ID_SEED --observer uploadStartupDirectory startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) startWebConnector
    fi
else
  exec ${LAUNCH_COMMAND} ${@}
fi