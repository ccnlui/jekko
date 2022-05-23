package jekko.echonode;

public abstract interface EchoNode
{
    public abstract void run();

    public abstract void close() throws Exception;
}
