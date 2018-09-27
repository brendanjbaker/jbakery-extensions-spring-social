package org.jbakery.extensions.springsocial;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jbakery.arguments.Argument;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.DuplicateConnectionException;
import org.springframework.social.connect.NoSuchConnectionException;
import org.springframework.social.connect.NotConnectedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class JdbcConnectionRepository
	implements ConnectionRepository
{
	private final ConnectionFactoryLocator connectionFactoryLocator;
	private final JdbcTemplate jdbcTemplate;
	private final ConnectionRowMapper mapper = new ConnectionRowMapper();
	private final SchemaConfiguration schemaConfiguration;
	private final TextEncryptor textEncryptor;
	private final String userId;

	public JdbcConnectionRepository(
		ConnectionFactoryLocator connectionFactoryLocator,
		JdbcTemplate jdbcTemplate,
		SchemaConfiguration schemaConfiguration,
		TextEncryptor textEncryptor,
		String userId)
	{
		this.connectionFactoryLocator = Argument.notNull(connectionFactoryLocator, "connectionFactoryLocator");
		this.jdbcTemplate = Argument.notNull(jdbcTemplate, "jdbcTemplate");
		this.schemaConfiguration = Argument.notNull(schemaConfiguration, "schemaConfiguration");
		this.textEncryptor = Argument.notNull(textEncryptor, "textEncryptor");
		this.userId = Argument.notNull(userId, "userId");
	}

	@Override
	public MultiValueMap<String, Connection<?>> findAllConnections()
	{
		final var query =
			String.format(
				"%s WHERE %s = ? AND %s = ? ORDER BY %s",
				selectAll(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getRankColumnName());

		final var results = jdbcTemplate.query(query, mapper, userId);
		final var connections = new LinkedMultiValueMap<String, Connection<?>>();
		final var registeredProviderIds = connectionFactoryLocator.registeredProviderIds();

		for (final var registeredProviderId : registeredProviderIds)
			connections.put(registeredProviderId, Collections.emptyList());

		for (final var connection : results)
		{
			final var providerId = connection.getKey().getProviderId();

			if (!connections.containsKey(providerId))
				connections.put(providerId, new LinkedList<>());

			connections.add(providerId, connection);
		}

		return connections;
	}

	@Override
	public List<Connection<?>> findConnections(final String providerId)
	{
		Argument.notNull(providerId, "providerId");

		final var query =
			String.format(
				"%s WHERE %s = ? AND %s = ? ORDER BY %s",
				selectAll(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getRankColumnName());

		return jdbcTemplate.query(query, mapper, userId, providerId);
	}

	@Override
	public <T> List<Connection<T>> findConnections(final Class<T> apiType)
	{
		Argument.notNull(apiType, "apiType");

		List<?> connections = findConnections(getProviderId(apiType));

		return (List<Connection<T>>)connections;
	}

	@Override
	public MultiValueMap<String, Connection<?>> findConnectionsToUsers(final MultiValueMap<String, String> providerUsers)
	{
		Argument.notNull(providerUsers, "providerUsers");

		if (providerUsers.isEmpty())
			throw new IllegalArgumentException("providerUsers");

		final var criteria = new StringBuilder();
		final var parameters = new MapSqlParameterSource();

		parameters.addValue("userId", userId);

		for (final var iterator = providerUsers.entrySet().iterator(); iterator.hasNext();)
		{
			final var entry = iterator.next();
			final var providerId = entry.getKey();

			criteria
				.append(String.format("%s = :providerId_%s AND ", schemaConfiguration.getProviderIdColumnName(), providerId))
				.append(String.format("%s IN (:providerUserIds_%s)", schemaConfiguration.getProviderUserIdColumnName(), providerId));

			parameters.addValue(String.format("providerId_%s", providerId), providerId);
			parameters.addValue(String.format("providerUserIds_%s", providerId), entry.getValue());

			if (iterator.hasNext())
				criteria.append(" OR ");
		}

		final var query =
			String.format(
				"%s WHERE %s = :userId AND %s ORDER BY %s, %s",
				selectAll(),
				schemaConfiguration.getUserIdColumnName(),
				criteria.toString(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getRankColumnName());

		final var queryTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		final var results = queryTemplate.query(query, parameters, mapper);
		final var connectionsForUsers = new LinkedMultiValueMap<String, Connection<?>>();

		for (final var connection : results)
		{
			final var providerId = connection.getKey().getProviderId();
			final var providerUserId = connection.getKey().getProviderUserId();
			final var userIds = providerUsers.get(providerId);
			final var connectionIndex = userIds.indexOf(providerUserId);

			var connections = connectionsForUsers.get(providerId);

			if (connections == null)
			{
				connections = new ArrayList<>(userIds.size());

				for (var i = 0; i < userIds.size(); i++)
					connections.add(null);

				connectionsForUsers.put(providerId, connections);
			}

			connections.set(connectionIndex, connection);
		}

		return connectionsForUsers;
	}

	@Override
	public Connection<?> getConnection(final ConnectionKey connectionKey)
	{
		Argument.notNull(connectionKey, "connectionKey");

		try
		{
			final var query =
				String.format(
					"%s WHERE %s = ? AND %s = ? AND %s = ?",
					selectAll(),
					schemaConfiguration.getUserIdColumnName(),
					schemaConfiguration.getProviderIdColumnName(),
					schemaConfiguration.getProviderUserIdColumnName());

			return jdbcTemplate.queryForObject(query, mapper, userId, connectionKey.getProviderId(), connectionKey.getProviderUserId());
		}
		catch (final EmptyResultDataAccessException e)
		{
			throw new NoSuchConnectionException(connectionKey);
		}
	}

	@Override
	public <T> Connection<T> getConnection(final Class<T> apiType, final String providerUserId)
	{
		Argument.notNull(apiType, "apiType");
		Argument.notNull(providerUserId, "providerUserId");

		final var providerId = getProviderId(apiType);
		final var connection = getConnection(new ConnectionKey(providerId, providerUserId));

		return (Connection<T>)connection;
	}

	@Override
	public <T> Connection<T> getPrimaryConnection(final Class<T> apiType)
	{
		Argument.notNull(apiType, "apiType");

		final var providerId = getProviderId(apiType);
		final var connection = findPrimaryConnection(apiType);

		if (connection == null)
			throw new NotConnectedException(providerId);

		return connection;
	}

	@Override
	public <T> Connection<T> findPrimaryConnection(final Class<T> apiType)
	{
		Argument.notNull(apiType, "apiType");

		final var providerId = getProviderId(apiType);
		final var connection = findPrimaryConnection(providerId);

		return (Connection<T>)connection;
	}

	@Override
	@Transactional
	public void addConnection(final Connection<?> connection)
	{
		Argument.notNull(connection, "connection");

		final var selectQuery =
			String.format(
				"SELECT COALESCE(MAX(%s) + 1, 1) AS %s FROM %s WHERE %s = ? AND %s = ?",
				schemaConfiguration.getRankColumnName(),
				schemaConfiguration.getRankColumnName(),
				schemaConfiguration.getTableName(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName());

		final var data = connection.createData();
		final var rank = jdbcTemplate.queryForObject(selectQuery, new Object[] { userId, data.getProviderId() }, Integer.class);

		final var insertQuery =
			String.format(
				"INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				schemaConfiguration.getTableName(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getProviderUserIdColumnName(),
				schemaConfiguration.getRankColumnName(),
				schemaConfiguration.getDisplayNameColumnName(),
				schemaConfiguration.getProfileUrlColumnName(),
				schemaConfiguration.getImageUrlColumnName(),
				schemaConfiguration.getAccessTokenColumnName(),
				schemaConfiguration.getSecretColumnName(),
				schemaConfiguration.getRefreshTokenColumnName(),
				schemaConfiguration.getExpireTimeColumnName());

		final var insertQueryParameters = new Object[]
		{
			userId,
			data.getProviderId(),
			data.getProviderUserId(),
			rank,
			data.getDisplayName(),
			data.getProfileUrl(),
			data.getImageUrl(),
			encrypt(data.getAccessToken()),
			encrypt(data.getSecret()),
			encrypt(data.getRefreshToken()),
			data.getExpireTime()
		};

		try
		{
			jdbcTemplate.update(insertQuery, insertQueryParameters);
		}
		catch (final DuplicateKeyException e)
		{
			throw new DuplicateConnectionException(connection.getKey());
		}
	}

	@Override
	@Transactional
	public void updateConnection(final Connection<?> connection)
	{
		Argument.notNull(connection, "connection");

		final var data = connection.createData();

		final var query =
			String.format(
				"UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ? AND %s = ? AND %s = ?",
				schemaConfiguration.getTableName(),
				schemaConfiguration.getDisplayNameColumnName(),
				schemaConfiguration.getProfileUrlColumnName(),
				schemaConfiguration.getImageUrlColumnName(),
				schemaConfiguration.getAccessTokenColumnName(),
				schemaConfiguration.getSecretColumnName(),
				schemaConfiguration.getRefreshTokenColumnName(),
				schemaConfiguration.getExpireTimeColumnName(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getProviderUserIdColumnName());

		final var queryParameters = new Object[]
		{
			data.getDisplayName(),
			data.getProfileUrl(),
			data.getImageUrl(),
			encrypt(data.getAccessToken()),
			encrypt(data.getSecret()),
			encrypt(data.getRefreshToken()),
			data.getExpireTime(),
			userId,
			data.getProviderId(),
			data.getProviderUserId()
		};

		jdbcTemplate.update(query, queryParameters);
	}

	@Override
	@Transactional
	public void removeConnections(final String providerId)
	{
		Argument.notNull(providerId, "providerId");

		final var query =
			String.format(
				"DELETE FROM %s WHERE %s = ? AND %s = ?",
				schemaConfiguration.getTableName(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName());

		final var queryParameters = new Object[]
		{
			userId,
			providerId
		};

		jdbcTemplate.update(query, queryParameters);
	}

	@Override
	@Transactional
	public void removeConnection(final ConnectionKey connectionKey)
	{
		Argument.notNull(connectionKey, "connectionKey");

		final var query =
			String.format(
				"DELETE FROM %s WHERE %s = ? AND %s = ? AND %s = ?",
				schemaConfiguration.getTableName(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getProviderUserIdColumnName());

		final var queryParameters = new Object[]
		{
			userId,
			connectionKey.getProviderId(),
			connectionKey.getProviderUserId()
		};

		jdbcTemplate.update(query, queryParameters);
	}

	private String decrypt(final String ciphertext)
	{
		if (ciphertext == null)
			return null;

		return textEncryptor.decrypt(ciphertext);
	}

	private String encrypt(final String plaintext)
	{
		if (plaintext == null)
			return null;

		return textEncryptor.encrypt(plaintext);
	}

	private Connection<?> findPrimaryConnection(final String providerId)
	{
		final var query =
			String.format(
				"%s WHERE %s = ? AND %s = ? ORDER BY %s",
				selectAll(),
				schemaConfiguration.getUserIdColumnName(),
				schemaConfiguration.getProviderIdColumnName(),
				schemaConfiguration.getRankColumnName());

		final var results = jdbcTemplate.query(query, mapper, userId, providerId);

		if (results.isEmpty())
			return null;

		return results.get(0);
	}

	private <T> String getProviderId(final Class<T> apiType)
	{
		return connectionFactoryLocator.getConnectionFactory(apiType).getProviderId();
	}

	private String selectAll()
	{
		return String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s, %s FROM %s",
			schemaConfiguration.getUserIdColumnName(),
			schemaConfiguration.getProviderIdColumnName(),
			schemaConfiguration.getProviderUserIdColumnName(),
			schemaConfiguration.getDisplayNameColumnName(),
			schemaConfiguration.getProfileUrlColumnName(),
			schemaConfiguration.getImageUrlColumnName(),
			schemaConfiguration.getAccessTokenColumnName(),
			schemaConfiguration.getSecretColumnName(),
			schemaConfiguration.getRefreshTokenColumnName(),
			schemaConfiguration.getExpireTimeColumnName(),
			schemaConfiguration.getTableName());
	}

	private final class ConnectionRowMapper
		implements RowMapper<Connection<?>>
	{
		@Override
		public Connection<?> mapRow(final ResultSet results, final int index)
			throws SQLException
		{
			final var connectionData =
				new ConnectionData(
					results.getString(schemaConfiguration.getProviderIdColumnName()),
					results.getString(schemaConfiguration.getProviderUserIdColumnName()),
					results.getString(schemaConfiguration.getDisplayNameColumnName()),
					results.getString(schemaConfiguration.getProfileUrlColumnName()),
					results.getString(schemaConfiguration.getImageUrlColumnName()),
					decrypt(results.getString(schemaConfiguration.getAccessTokenColumnName())),
					decrypt(results.getString(schemaConfiguration.getSecretColumnName())),
					decrypt(results.getString(schemaConfiguration.getRefreshTokenColumnName())),
					nullIfZero(results.getLong(schemaConfiguration.getExpireTimeColumnName())));

			final var connectionFactory = connectionFactoryLocator.getConnectionFactory(connectionData.getProviderId());

			return connectionFactory.createConnection(connectionData);
		}

		private Long nullIfZero(final long value)
		{
			if (value == 0)
				return null;

			return value;
		}
	}
}
