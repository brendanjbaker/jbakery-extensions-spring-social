package org.jbakery.extensions.springsocial;

import java.util.List;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.util.MultiValueMap;

public class JdbcConnectionRepository
	implements ConnectionRepository
{
	@Override
	public MultiValueMap<String, Connection<?>> findAllConnections()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<Connection<?>> findConnections(String providerId)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <A> List<Connection<A>> findConnections(Class<A> apiType)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public MultiValueMap<String, Connection<?>> findConnectionsToUsers(MultiValueMap<String, String> providerUsers)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Connection<?> getConnection(ConnectionKey connectionKey)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <A> Connection<A> getConnection(Class<A> apiType, String providerUserId)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <A> Connection<A> getPrimaryConnection(Class<A> apiType)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <A> Connection<A> findPrimaryConnection(Class<A> apiType)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void addConnection(Connection<?> connection)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void updateConnection(Connection<?> connection)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeConnections(String providerId)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeConnection(ConnectionKey connectionKey)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
