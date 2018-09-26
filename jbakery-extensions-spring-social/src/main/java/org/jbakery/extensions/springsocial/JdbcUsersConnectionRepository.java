package org.jbakery.extensions.springsocial;

import java.util.List;
import java.util.Set;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;

public class JdbcUsersConnectionRepository
	implements UsersConnectionRepository
{
	@Override
	public List<String> findUserIdsWithConnection(Connection<?> connection)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ConnectionRepository createConnectionRepository(String userId)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setConnectionSignUp(ConnectionSignUp connectionSignUp)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
