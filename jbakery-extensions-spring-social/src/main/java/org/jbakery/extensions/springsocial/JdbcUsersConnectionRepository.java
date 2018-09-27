package org.jbakery.extensions.springsocial;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Set;
import javax.sql.DataSource;
import org.jbakery.arguments.Argument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;

public final class JdbcUsersConnectionRepository
	implements UsersConnectionRepository
{
	private final ConnectionFactoryLocator connectionFactoryLocator;
	private final DataSource dataSource;
	private final SchemaConfiguration schemaConfiguration;
	private final TextEncryptor textEncryptor;

	private ConnectionSignUp connectionSignUp;
	private JdbcTemplate jdbcTemplate;

	public JdbcUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator, DataSource dataSource, SchemaConfiguration schemaConfiguration, TextEncryptor textEncryptor)
	{
		this.connectionFactoryLocator = Argument.notNull(connectionFactoryLocator, "connectionFactoryLocator");
		this.dataSource = Argument.notNull(dataSource, "dataSource");
		this.schemaConfiguration = Argument.notNull(schemaConfiguration, "schemaConfiguration");
		this.textEncryptor = Argument.notNull(textEncryptor, "textEncryptor");
	}

	@Override
	public List<String> findUserIdsWithConnection(final Connection<?> connection)
	{
		Argument.notNull(connection, "connection");

		final var query =
			String.format(
				"SELECT %s FROM %s WHERE %s = ? AND %s = ?",
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getTableName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getProviderUserIdColumnName());

		// Query the database for matching user(s).

		final var userIds =
			getJdbcTemplate().queryForList(
				query,
				String.class,
				connection.getKey().getProviderId(),
				connection.getKey().getProviderUserId());

		// If we found matching user(s), return their IDs.

		if (!userIds.isEmpty())
			return userIds;

		// If there is no sign-up mechanism, return an empty set of user IDs.

		if (connectionSignUp == null)
			return emptyUserIdList();

		// Try signing up the user.

		final var userId = connectionSignUp.execute(connection);

		// If sign-up didn't succeed, return an empty set of user IDs.

		if (userId == null)
			return emptyUserIdList();

		// Register this connection for the new user.

		createConnectionRepository(userId).addConnection(connection);

		// Return the new user ID.

		return Arrays.asList(userId);
	}

	@Override
	public Set<String> findUserIdsConnectedTo(final String providerId, final Set<String> providerUserIds)
	{
		Argument.notNull(providerId, "providerId");
		Argument.notNull(providerUserIds, "providerUserIds");

		final var query =
			String.format(
				"SELECT %s FROM %s WHERE %s = :providerId AND %s IN (:providerUserIds)",
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getTableName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getProviderUserIdColumnName());

		final var queryParameters =
			new MapSqlParameterSource(
				Map.ofEntries(
					entry("providerId", providerId),
					entry("providerUserIds", providerUserIds)));

		final var queryTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate());

		return queryTemplate.query(query, queryParameters, results ->
		{
			final var userIds = new HashSet<String>();

			while (results.next())
				userIds.add(results.getString(schemaConfiguration.getUserIdColumnName()));

			return userIds;
		});
	}

	@Override
	public ConnectionRepository createConnectionRepository(final String userId)
	{
		Argument.notNull(userId, "userId");

		return new JdbcConnectionRepository(connectionFactoryLocator, getJdbcTemplate(), schemaConfiguration, textEncryptor, userId);
	}

	@Override
	public void setConnectionSignUp(final ConnectionSignUp connectionSignUp)
	{
		this.connectionSignUp = Argument.notNull(connectionSignUp, "connectionSignUp");
	}

	private JdbcTemplate getJdbcTemplate()
	{
		// If the variable is already initialized, return it.

		if (jdbcTemplate != null)
			return jdbcTemplate;

		// The variable isn't initialized; enter synchronization block.

		synchronized (this)
		{
			// If another thread initialized the variable while we were waiting, return it.

			if (jdbcTemplate != null)
				return jdbcTemplate;

			// Initialize the variable.

			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		return jdbcTemplate;
	}

	private static List<String> emptyUserIdList()
	{
		return Collections.emptyList();
	}
}
