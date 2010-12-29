// Copyright 2008,2009,2010 Massachusetts Institute of Technology.
// All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "dtxn/configparser.h"
#include "io/libeventloop.h"
#include "net/messageconnectionutil.h"
#include "net/messageserver.h"
#include "dtxn/ordered/ordereddtxnmanager.h"
#include "protodtxn/protodtxncoordinator.h"
#include "protorpc/protoserver.h"
#include "base/debuglog.h"

using std::vector;

int main(int argc, const char* argv[]) {
    if (argc != 3) {
        fprintf(stderr, "protodtxncoordinator [listen port] [configuration file]\n");
        return 1;
    }
    int port = atoi(argv[1]);
    assert(port != 0);
    const char* const config_file = argv[2];

    vector<dtxn::Partition> partitions = dtxn::parseConfigurationFromPath(config_file);

    io::LibEventLoop event_loop;

    fprintf(stderr, "LET'S DO THIS\n");
    
    // Connect to the backends
    vector<TCPConnection*> tcp_connections = net::createConnectionsWithRetry(
            &event_loop, primaryAddresses(partitions));
    if (tcp_connections.empty()) {
        fprintf(stderr, "some connections failed\n");
        return 1;
    }
    
    fprintf(stderr, "OK I'M READY\n");
    
    ASSERT(tcp_connections.size() == partitions.size());
    vector<MessageConnection*> msg_connections(tcp_connections.size());
    for (int i = 0; i < msg_connections.size(); ++i) {
        msg_connections[i] = new TCPMessageConnection(event_loop.base(), tcp_connections[i]);
    }
    tcp_connections.clear();

    // Create the manager to serve DTXN stuff    
    net::MessageServer msg_server;
    vector<net::ConnectionHandle*> connections = msg_server.addConnections(msg_connections);
    msg_connections.clear();
    dtxn::OrderedDtxnManager manager(&event_loop, &msg_server, connections);
    protodtxn::ProtoDtxnCoordinator coordinator(&manager, (int) partitions.size());

    // Serve the coordinator via ProtoRPC
    protorpc::ProtoServer rpc_server(&event_loop);
    rpc_server.registerService(&coordinator);
    rpc_server.listen(port);
    printf("listening on port %d\n", port);

    event_loop.exitOnSigInt(true);
    event_loop.run();

    return 0;
}
