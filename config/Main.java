public static void main(String[] args) throws Exception {
    ServerConf conf = new ServerConf("config/server.conf");
    System.out.println("listen=" + conf.port);
    System.out.println(conf.host2root);
}
